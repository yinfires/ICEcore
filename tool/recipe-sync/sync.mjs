import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const defaults = {
  source: 'D:/Minecraft/Mod Develop/recipe-manager/public/data/recipe-manager.json',
  client: 'D:/Minecraft/PCL启动器/.minecraft/versions/异次元餐厅',
};
const encode = value => JSON.stringify(value, null, 2) + '\n';
const digest = value => crypto.createHash('sha256').update(value).digest('hex');
const read = (file, fallback) => fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '')) : fallback;
const mainTags = {tag_food_main_staple:'main', tag_food_main_drink:'drink', tag_food_main_snack:'dessert'};
const roles = ['tag_1790130836441','tag_1790252241364','tag_1790175849511'];
export function price(item) {
  const value = item.manualPrice ?? item.autoPrice;
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) throw Error('Missing/invalid price');
  return Math.round(value);
}
function tagContains(resources,tag,item,seen=new Set()){
  if(seen.has(tag))return false;seen.add(tag);const [ns,p]=tag.split(':');
  return (resources[`data/${ns}/tags/items/${p}.json`]?.values??[]).some(v=>typeof v==='string'&&(v===item||v.startsWith('#')&&tagContains(resources,v.slice(1),item,seen)));
}
export function jarResources(client) {
  const resources={}; const provenance=[]; const folder=path.join(client,'mods');
  Object.defineProperty(resources,'provenance',{value:provenance,enumerable:false});
  if(!fs.existsSync(folder))return resources;
  const cache=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../tmp/recipe-sync-resources');
  fs.mkdirSync(cache,{recursive:true});
  for(const name of fs.readdirSync(folder).filter(n=>n.endsWith('.jar')&&/kaleido|youkai|farmersdelight|farm_and_charm|aquaculture|collectorsreap|crabbersdelight|extradelight|bakery|icecore/i.test(n)).sort()) {
    const source=path.join(folder,name), hash=digest(fs.readFileSync(source));
    provenance.push({name,sha256:hash});
    const extracted=path.join(cache,hash.slice(0,20)), complete=path.join(extracted,'.complete');
    if(!fs.existsSync(complete)) {
      const zip=extracted+'.zip';fs.copyFileSync(source,zip);
      const quote=s=>"'"+s.replaceAll("'","''")+"'";
      const powershell=process.env.PWSH_PATH??'pwsh.exe';
      execFileSync(powershell,['-NoProfile','-NonInteractive','-Command',
        `$ErrorActionPreference='Stop'; Expand-Archive -LiteralPath ${quote(zip)} -DestinationPath ${quote(extracted)} -Force`],{timeout:120000,stdio:'pipe'});
      fs.writeFileSync(complete,hash);fs.rmSync(zip);
    }
    const visit=directory=>{
      if(!fs.existsSync(directory))return;
      for(const entry of fs.readdirSync(directory,{withFileTypes:true})){
        const file=path.join(directory,entry.name);
        if(entry.isDirectory())visit(file);
        else if(entry.name.endsWith('.json')){
          const relative=path.relative(extracted,file).split(path.sep).join('/');
          // Only resources consumed by conversion; unrelated locale errors must not block sync.
          if(!/^data\/[^/]+\/(recipes|tags\/items|grilling)\/.+\.json$/.test(relative)
              && !/^assets\/[^/]+\/lang\/(zh_cn|en_us)\.json$/.test(relative))continue;
          try{
            const json=read(file);
            resources[relative]=relative.includes('/lang/')?{...resources[relative],...json}:json;
          }catch(error){throw Error(`Invalid resource ${name}:${relative}: ${error.message}`);}
        }
      }
    };
    visit(path.join(extracted,'assets'));visit(path.join(extracted,'data'));
  } return resources;
}
export function compile(document, resources={}) {
  const data = document.data, files = {}, results = [], itemIssues=[], tagIssues=[], containers = new Map();
  const items = data.items;
  const languageKeys=new Set(Object.entries(resources).filter(([p])=>p.includes('/lang/')).flatMap(([,json])=>Object.keys(json)));
  const resolve = ref => {const item=items[ref]; if(!item?.itemId || !/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(item.itemId) || item.itemId.includes('..')) throw Error('Unknown item: '+ref); return item;};
  const entries = list => (list??[]).map(v=> {if(v.type!=='item' || !Number.isInteger(v.count)||v.count<1)throw Error('Unsupported ingredient/count');return {item:resolve(v.ref).itemId,count:v.count};});
  const expand = list => list.flatMap(v=>Array.from({length:v.count},()=>({item:v.item})));
  const fluidOf = id => ({'minecraft:water_bucket':'minecraft:water','minecraft:milk_bucket':'minecraft:milk','kaleidoscope_grilling:canola_oil_bucket':'kaleidoscope_grilling:canola_oil'}[id]);
  const put=(p,value)=>{files[p]=encode(value);};
  for(const recipe of Object.values(data.recipes)) {
    const priorFiles={...files}, priorContainers=new Map(containers);
    try {
      if(!/^[a-z0-9_-]+$/.test(recipe.id))throw Error('Unsafe recipe ID');
      const workstation=resolve(recipe.workstation).itemId, input=entries(recipe.inputs), attachment=entries(recipe.attachments), output=entries(recipe.outputs);
      if(!output.length)throw Error('No outputs');
      const carrier=['minecraft:stick','kaleidoscope_grilling:grill','farmersdelight:cutting_board'].includes(workstation)
        ?[]:attachment.filter(v=>!fluidOf(v.item));
      if(carrier.length>1 || carrier.some(v=>v.count!==1))throw Error('Multiple/non-unit carriers require confirmation');
      const result=output[0]; let json, native=false;
      const ingredients=expand(input);
      const single=()=>{if(input.length!==1||input[0].count!==1||output.length!==1)throw Error('Requires single input/output');};
      const carrierFields=carrier.length?{carrier:{item:carrier[0].item}}:{};
      switch(workstation) {
        case 'farmersdelight:cutting_board':
          if(input.length!==1||input[0].count!==1)throw Error('Cutting requires one input'); json={type:'farmersdelight:cutting',ingredients:[{item:input[0].item}],tool:{item:attachment[0]?.item??'kaleidoscope_cookery:iron_kitchen_knife'},result:output.map(v=>({...v,chance:1}))}; break;
        case 'kaleidoscope_cookery:bamboo_tray': single();json={type:'kaleidoscope_cookery:bamboo_tray',subtype:'drying',duration:1800,ingredient:{item:input[0].item},result};break;
        case 'kaleidoscope_cookery:steamer': single();json={type:'kaleidoscope_cookery:steamer',cook_tick:600,ingredient:{item:input[0].item},result};break;
        case 'icecore:oven':
          if(ingredients.length>9 || expand(output).length>9)throw Error('Oven capacity exceeded');
          json={type:'icecore:oven',ingredients,results:expand(output),cooking_time:400,processing:{mode:'batch',max_batches:9}};break;
        case 'icecore:mixing_bowl': {
          const liquids=input.filter(v=>fluidOf(v.item)); if(liquids.length>1)throw Error('Multiple fluids');
          const solids=expand(input.filter(v=>!fluidOf(v.item)));
          if(solids.length+liquids.length>9)throw Error('Mixing input capacity exceeded');
          const seasoning=result.item==='kaleidoscope_grilling:special_seasoning';
          if(seasoning && (output.length!==1 || result.count!==16))throw Error('Seasoning requires sixteen native uses per bottle');
          const produced=seasoning?[{item:result.item,nbt:{SeasoningUses:0,SeasoningIngredients:solids.map(v=>v.item)}}]:expand(output);
          if(produced.length>9)throw Error('Mixing output capacity exceeded');
          json={type:'icecore:mixing_bowl',ingredients:solids,results:produced.map(v=>({...v,...carrierFields}))};
          if(liquids.length)json.fluid={fluid:fluidOf(liquids[0].item),amount:liquids[0].count*1000,display:{item:liquids[0].item}};
          break;
        }
        case 'kaleidoscope_cookery:pot': json={type:workstation,ingredients,result,...carrierFields,time:200,stir_fry_count:3};break;
        case 'kaleidoscope_cookery:stockpot': {
          const liquid=attachment.filter(v=>fluidOf(v.item));if(liquid.length!==1)throw Error('Missing soup base');
          json={type:workstation,ingredients,result,...carrierFields,...(!carrier.length?{empty_carrier:true}:{}),soup_base:liquid[0].item==='kaleidoscope_grilling:canola_oil_bucket'?'icecore:canola_oil':fluidOf(liquid[0].item),time:300};break;
        }
        case 'kaleidoscope_cookery:teapot': {
          const tea=input.filter(v=>!fluidOf(v.item)), liquid=input.filter(v=>fluidOf(v.item));
          if(tea.length!==1||liquid.length!==1||liquid[0].count!==1||result.count!==12)throw Error('Unsupported tea quantities');
          json={type:workstation,ingredient:{item:tea[0].item},ingredient_count:tea[0].count,result:{item:result.item},tea_fluid:fluidOf(liquid[0].item),time:240};break;
        }
        case 'kaleidoscope_world_liquor:freezer': {
          const liquid=input.filter(v=>fluidOf(v.item));if(liquid.length>1)throw Error('Multiple freezer fluids');
          if(ingredients.length-liquid.reduce((n,v)=>n+v.count,0)>4)throw Error('Freezer requires counted ingredient adapter');
          json={type:workstation,ingredients:expand(input.filter(v=>!fluidOf(v.item))),fluid:liquid.length?fluidOf(liquid[0].item):'minecraft:empty',fluid_amount:liquid.length?liquid[0].count*1000:0,result,craft_time:1800,texture:'minecraft:block/ice',...(carrier.length?{extract_condition:{item:carrier[0].item}}:{})};break;
        }
        case 'youkaisfeasts:fermentation_tank': {
          const liquid=input.filter(v=>fluidOf(v.item));if(liquid.length>1)throw Error('Multiple ferment fluids');
          json={type:'youkaisfeasts:simple_fermentation',ingredients:expand(input.filter(v=>!fluidOf(v.item))),inputFluid:{fluid:liquid.length?fluidOf(liquid[0].item):'minecraft:empty',amount:liquid.length?liquid[0].count*1000:0},outputFluid:{fluid:'minecraft:empty',amount:0},results:output,time:6000};break;
        }
        case 'kaleidoscope_fragrantorchard:juicer': {
          const liquid=input.filter(v=>fluidOf(v.item));if(liquid.length>1)throw Error('Multiple juicer fluids');
          if(!carrier.length)throw Error('Missing juicer container');
          const bucket=/^kaleidoscope_tavern:(.+)_bucket$/.exec(result.item);
          const nativeJuice=Object.values(resources).find(v=>v?.type===workstation && v.mode!=='mixed' && v.extract?.result===result.item);
          const juiceFluid=bucket?'kaleidoscope_tavern:'+bucket[1]+'_juice':nativeJuice?.output?.fluid
            ??(result.item==='extradelight:lemon_juice'?'extradelight:lemon_juice_fluid':null);
          if(!liquid.length && input.length===1 && juiceFluid) {
            const cost=bucket?1000:250;
            const amount=cost*result.count/input[0].count;
            if(!Number.isInteger(amount)||amount<1)throw Error('Non-integral juice yield');
            json={type:workstation,mode:'juice',time:200,input:{item:input[0].item,fluid:amount},output:{fluid:juiceFluid},
              ...(!bucket?{extract:{container:carrier[0].item,fluid:cost,result:result.item}}:{})};
          } else {
            const solids=expand(input.filter(v=>!fluidOf(v.item)));
            if(solids.length>5)throw Error('Juicer input capacity exceeded');
            // JuicerBlock fills milk buckets with its own registered milk, unlike other devices.
            const fluid=liquid.length?(liquid[0].item==='minecraft:milk_bucket'?'kaleidoscope_fragrantorchard:milk_still':fluidOf(liquid[0].item)):'minecraft:empty';
            json={type:workstation,mode:'mixed',time:200,input:{fluid,amount:liquid.length?liquid[0].count*1000:0,items:solids},output:{...result,container:carrier[0].item}};
          }break;
        }
        case 'kaleidoscope_tavern:barrel': {
          const liquid=input.filter(v=>v.item.endsWith('_bucket'));if(liquid.length!==1||liquid[0].count!==4||result.count!==16)throw Error('Expected four buckets and sixteen bottles');
          const base=liquid[0].item==='minecraft:milk_bucket'?'minecraft:milk':liquid[0].item.replace('_bucket','_juice').replace('sweet_berries_juice','sweet_berries_juice');
          const solid=input.filter(v=>v!==liquid[0]);if(solid.some(v=>v.count!==16)||solid.length>4)throw Error('Barrel ingredient ratio');
          json={type:workstation,fluid:base,ingredients:solid.map(v=>({item:v.item})),...carrierFields,result:{item:result.item},unit_time:2400};break;
        }
        case 'youkaisfeasts:cuisine_board': {
          const originals=Object.entries(resources).filter(([p,j])=>p.includes('/recipes/')&&j.type?.startsWith('youkaisfeasts:cuisine_')&&j.result?.item===result.item);
          if(result.item==='aquaculturedelight:fried_perch_roll'&&!originals.length)originals.push(['verified_sushi',{type:'youkaisfeasts:cuisine_ordered',base:'youkaisfeasts:sushi'}]);
          if(originals.length!==1)throw Error('Missing/ambiguous native cuisine base');
          const original=originals[0][1], remaining=expand(input).map(v=>v.item);
          let base=original.base;
          const preparedRoe=base==='youkaisfeasts:california_roll'&&!remaining.includes(base)&&remaining.includes('youkaisfeasts:roe_california_roll');
          if(preparedRoe)base='youkaisfeasts:roe_california_roll';
          const consume=id=>{const index=remaining.indexOf(id);if(index<0)throw Error('Missing native base ingredient '+id);remaining.splice(index,1);};
          const rice=Object.values(items).find(i=>i.itemId==='farmersdelight:cooked_rice')?.itemId;
          if(base==='youkaisfeasts:sushi')consume(rice);
          else if(base==='youkaisfeasts:gunkan'){consume(rice);consume('minecraft:dried_kelp');}
          else if(base==='youkaisfeasts:hosomaki'){consume('minecraft:dried_kelp');consume(rice);}
          else if(['youkaisfeasts:futomaki','youkaisfeasts:california'].includes(base)){consume(rice);consume(rice);consume('minecraft:dried_kelp');}
          else consume(base);
          if(!remaining.length)throw Error('No cuisine toppings');
          if(original.type==='youkaisfeasts:cuisine_mixed'){
            // Roe is already part of the supplied completed base; do not charge it twice.
            const first=preparedRoe?[]:original.first??[], ordered=[];
            for(const selector of first){const index=remaining.findIndex(id=>selector.item===id || selector.tag&&tagContains(resources,selector.tag,id));if(index<0)throw Error('Missing native first-stage ingredient');ordered.push({item:remaining.splice(index,1)[0]});}
            json={type:original.type,base,first:ordered,second:remaining.map(item=>({item})),result};
          }else json={type:'youkaisfeasts:cuisine_ordered',base,input:remaining.map(item=>({item})),result};
          break;
        }
        case 'minecraft:stick': {
          const raw=result.item;
          const cooked=Object.values(data.recipes).find(r=>items[r.workstation]?.itemId==='kaleidoscope_grilling:grill'&&items[r.inputs[0]?.ref]?.itemId===raw);
          const slots=expand(input.filter(v=>v.item!=='minecraft:stick')).map(v=>[v.item]);
          if(input.filter(v=>v.item==='minecraft:stick').reduce((n,v)=>n+v.count,0)!==1)throw Error('Requires one stick');
          const original=resources['data/kaleidoscope_grilling/grilling/skewers.json']?.skewers?.find(v=>v.id===raw);
          put('kubejs/data/icecore/grilling/recipe_manager/'+recipe.id+'.json',{skewers:[{...original,id:raw,ingredients:slots,...(cooked?{cooked_result:resolve(cooked.outputs[0].ref).itemId}:{})}]});
          native=true;break;
        }
        case 'kaleidoscope_grilling:grill': {
          single();const raw=input[0].item;
          const threading=Object.values(data.recipes).find(r=>items[r.workstation]?.itemId==='minecraft:stick'&&items[r.outputs[0]?.ref]?.itemId===raw);
          if(!threading)throw Error('No threading recipe');
          native=true;break;
        }
        case 'kaleidoscope_grilling:oil_press':
          if(input.length!==1 || input[0].item!=='kaleidoscope_grilling:oil_cake' || input[0].count!==1
              || output.length!==1 || result.item!=='kaleidoscope_grilling:canola_oil_bucket' || result.count!==1
              || carrier.length!==1 || carrier[0].item!=='minecraft:bucket')throw Error('Native oil press requires one cake per oil bucket');
          native=true;break;
        default: throw Error('Device adapter not verified: '+workstation);
      }
      if(output.length>1 && !['farmersdelight:cutting_board','icecore:oven','icecore:mixing_bowl','youkaisfeasts:fermentation_tank'].includes(workstation))throw Error('Unsupported multiple outputs');
      const id='icecore:recipe_manager/'+recipe.id;
      if(!native)put('kubejs/data/icecore/recipes/recipe_manager/'+recipe.id+'.json',json);
      if(workstation!=='farmersdelight:cutting_board' && carrier.length)for(const out of output){const previous=containers.get(out.item);if(previous&&previous!==carrier[0].item)throw Error('Conflicting carriers for '+out.item);containers.set(out.item,carrier[0].item);}
      results.push({id:recipe.id,name:recipe.name,status:'generated',native,recipeId:id,outputs:output.map(v=>v.item),
        artifacts:[...(!native?['kubejs/data/icecore/recipes/recipe_manager/'+recipe.id+'.json']:[]),
          ...(workstation==='minecraft:stick'?['kubejs/data/icecore/grilling/recipe_manager/'+recipe.id+'.json']:[]),
          ...(carrier.length?output.map(v=>'kubejs/data/icecore/icecore/container_compat/recipe_manager/'+v.item.replace(':','/')+'.json'):[])]});
    } catch(error){
      for(const key of Object.keys(files))if(!Object.hasOwn(priorFiles,key))delete files[key];
      Object.assign(files,priorFiles);containers.clear();for(const pair of priorContainers)containers.set(...pair);
      results.push({id:recipe.id,name:recipe.name,status:'skipped',reason:error.message});
    }
  }
  const names={}, metadata={items:{}}, tagValues={};
  for(const item of Object.values(items)) {
    const id=item.itemId;if(!id)continue;
    if(!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(id) || id.includes('..')) {
      itemIssues.push({id:item.id,itemId:id,reason:'Invalid item ID'});continue;
    }
    const [ns,p]=id.split(':');
    // Both item/block keys are intentional: BlockItems use their block translation ID.
    if(id!=='kaleidoscope_grilling:special_seasoning') {
      const name=id==='kaleidoscope_grilling:empty_seasoning_bottle'?'空调料瓶':item.name;
      names[`item.${ns}.${p.replaceAll('/','.')}`]=name;
      names[`block.${ns}.${p.replaceAll('/','.')}`]=name;
      // Native placeable-food items can delegate their title to a distinct block ID.
      const blockAlias=`block.${ns}.${p.replaceAll('/','.')}_block`;
      if(languageKeys.has(blockAlias))names[blockAlias]=name;
    }
    const tags=(item.tags??[]).filter(t=>{if(data.tags[t])return true;tagIssues.push({itemId:id,tag:t});return false;}), isFood=tags.includes(roles[2]), isIngredient=tags.some(t=>roles.slice(0,2).includes(t));
    for(const tag of tags){const stable=tag.replace(/^tag_/,'');(tagValues[stable]??=[]).push(id);}
    if(!isFood&&!isIngredient)continue;
    const main=Object.keys(mainTags).filter(t=>tags.includes(t));
    let salePrice;
    try {
      if(isFood&&main.length!==1)throw Error('Expected one food category');
      if(isFood)salePrice=price(item);
    } catch(error){itemIssues.push({id:item.id,itemId:id,reason:error.message});continue;}
    const category=isFood?mainTags[main[0]]:'ingredient';
    metadata.items[id]={category,tags:tags.filter(t=>!roles.includes(t)&&!mainTags[t]).map(t=>({translate:'tag.icecore.recipe_manager.'+t})),...(isFood?{price:salePrice}:{})};
    if(isFood)put(`kubejs/data/cozycafe/menu/${ns}/${p}.json`,{item:id,category,price:salePrice,themes:[],flavors:[]});
  }
  names['item.kaleidoscope_cookery.iron_kitchen_knife']='菜刀';
  names['block.kaleidoscope_grilling.seasoning_bottle']='空调料瓶';
  names['fluid.kaleidoscope_fragrantorchard.milk']='牛奶';
  for(const tag of Object.values(data.tags))names['tag.icecore.recipe_manager.'+tag.id]=tag.name;
  // A language file must use the selected locale name; merge is handled by the transaction layer.
  put('kubejs/assets/icecore/lang/zh_cn.json',names);
  // Minecraft loads languages namespace by namespace. Cross-namespace keys in icecore
  // can be overwritten later by the owner's namespace; override in that namespace.
  for(const ns of new Set(Object.keys(names).filter(k=>/^(item|block|fluid)\./.test(k)).map(k=>k.split('.')[1]).filter(ns=>ns!=='icecore')))
    put(`kubejs/assets/${ns}/lang/zh_cn.json`,Object.fromEntries(Object.entries(names).filter(([k])=>['item','block','fluid'].some(type=>k.startsWith(type+'.'+ns+'.')))));
  put('config/icecore/recipe_manager_items.json',metadata);
  put('config/icecore/item_descriptions.json',{items:Object.fromEntries([
    ...Object.keys(metadata.items), 'kaleidoscope_cookery:oil_pot'
  ].map(id=>[id,[]]))});
  for(const [tag,values]of Object.entries(tagValues))put(`kubejs/data/icecore/tags/items/recipe_manager/${tag}.json`,{replace:true,values:[...new Set(values)].sort()});
  const cuisineInputs=new Set(Object.values(data.recipes).filter(r=>items[r.workstation]?.itemId==='youkaisfeasts:cuisine_board')
    .flatMap(r=>(r.inputs??[]).map(v=>items[v.ref]?.itemId)));
  for(const [variant,item]of Object.entries({carrot:'kaleidoscope_grilling:carrot_dice',mayonnaise:'youkaisfeasts:bowl_of_cream'}))
    if(cuisineInputs.has(item))put(`kubejs/data/icecore/tags/items/cuisine_model_alias/${variant}.json`,{replace:true,values:[item]});
  const foodBases=new Set(['minecraft:bread','minecraft:bamboo','minecraft:pumpkin','minecraft:stick']);
  const plateRequired=results.filter(r=>r.status==='generated' && (data.recipes[r.id].attachments??[])
    .some(v=>foodBases.has(items[v.ref]?.itemId))).flatMap(r=>r.outputs);
  put('kubejs/data/icecore/tags/items/recipe_manager/food_base_carrier.json',{replace:true,values:[...new Set(plateRequired)].sort()});
  for(const result of results.filter(r=>r.status==='generated')) {
    const r=data.recipes[result.id];if(items[r.workstation].itemId!=='kaleidoscope_cookery:teapot')continue;
    containers.set(resolve(r.outputs[0].ref).itemId,resolve(r.attachments[0].ref).itemId);
  }
  const bucketFluids={
    'kaleidoscope_grilling:canola_oil_bucket':'kaleidoscope_grilling:canola_oil',
    ...Object.fromEntries(['grape','ice_grape','gold_grape','green_grape','sweet_berries','glow_berries']
      .map(name=>['kaleidoscope_tavern:'+name+'_bucket','kaleidoscope_tavern:'+name+'_juice']))
  };
  for(const [filled,empty]of containers)put(`kubejs/data/icecore/icecore/container_compat/recipe_manager/${filled.replace(':','/')}.json`,
    bucketFluids[filled]?{filled,empty,category:'bucket',behavior:'fluid',fluid:bucketFluids[filled],amount:1000}
      :{filled,empty,category:'container',behavior:'ingredient'});
  const protectedOutputs=new Set(results.filter(r=>r.status==='skipped').flatMap(r=>(data.recipes[r.id]?.outputs??[]).map(v=>items[v.ref]?.itemId)));
  const outputs=[...new Set(results.filter(r=>r.status==='generated').flatMap(r=>r.outputs))].filter(id=>!protectedOutputs.has(id));
  files['kubejs/server_scripts/icecore_recipe_manager.js']=removalScript(outputs);
  return {files,results,itemIssues,tagIssues};
}

function removalScript(outputs, ids=[]) {
  return `// Generated by ICEcore recipe-sync.\nServerEvents.afterRecipes(function(event) {\n  var outputs = ${JSON.stringify([...new Set(outputs)].sort())};\n  outputs.forEach(function(item) { event.remove({output:item,not:{id:/^icecore:recipe_manager\\//}}); });\n  ${JSON.stringify(ids)}.forEach(function(id) { event.remove({id:id}); });\n});\n`;
}

export function run(command,options={}) {
  const cfg={...defaults,...options}; const stateDir=path.join(cfg.client,'config/icecore/recipe-sync');
  const manifestPath=path.join(stateDir,'manifest.json'), previous=read(manifestPath,{files:{},results:[]});
  if(command==='rollback') {
    const backup=read(path.join(stateDir,'rollback.json'),null);if(!backup)throw Error('No rollback available');
    // Refuse rollback over edits made after apply, before changing any file.
    for(const relative of Object.keys(backup.files)) {
      const target=path.resolve(cfg.client,relative);
      if(!target.startsWith(path.resolve(cfg.client)+path.sep))throw Error('Unsafe path');
      const actual=fs.existsSync(target)?fs.readFileSync(target,'utf8'):null;
      if(actual!==(previous.files[relative]??null))throw Error('Refusing to rollback manually edited file: '+relative);
    }
    for(const [relative,content]of Object.entries(backup.files)){const target=path.resolve(cfg.client,relative);if(!target.startsWith(path.resolve(cfg.client)+path.sep))throw Error('Unsafe path');if(content===null)fs.rmSync(target,{force:true});else{fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,content);}}
    fs.writeFileSync(manifestPath,encode(backup.manifest)); return {rolledBack:true};
  }
  const raw=fs.readFileSync(cfg.source,'utf8'), document=JSON.parse(raw.replace(/^\uFEFF/,'')), resources=jarResources(cfg.client), compiled=compile(document,resources);
  const metadataPath='config/icecore/recipe_manager_items.json';
  const metadata=JSON.parse(compiled.files[metadataPath]);
  const oldMetadata=previous.files[metadataPath]?JSON.parse(previous.files[metadataPath]):{items:{}};
  for(const issue of compiled.itemIssues) {
    if(oldMetadata.items[issue.itemId])metadata.items[issue.itemId]=oldMetadata.items[issue.itemId];
    if(!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(issue.itemId??'') || issue.itemId.includes('..'))continue;
    const menu='kubejs/data/cozycafe/menu/'+issue.itemId.replace(':','/')+'.json';
    if(previous.files[menu])compiled.files[menu]=previous.files[menu];
  }
  compiled.files[metadataPath]=encode(metadata);
  const ownedJson={};
  for(const relative of [...new Set([...Object.keys(compiled.files),...Object.keys(previous.ownedJson??{})].filter(p=>/^kubejs\/assets\/[^/]+\/lang\/zh_cn\.json$/.test(p))),'config/icecore/item_descriptions.json']){
    const description=relative.endsWith('item_descriptions.json');
    const existing=read(path.join(cfg.client,relative),description?{items:{}}:{});
    const contents=description?existing.items:existing;
    const owned=previous.ownedJson?.[relative]??{};
    for(const [key,value]of Object.entries(owned))if(encode(contents[key])===encode(value))delete contents[key];
    const generated=JSON.parse(compiled.files[relative]??'{}');
    const next=description?generated.items:generated;
    // Description entries contain independently authored placement/actions. Keep them
    // and add only an empty replacement body when no manual entry exists.
    if(description)for(const key of Object.keys(next))if(Object.hasOwn(contents,key))delete next[key];
    if(!description)for(const key of Object.keys(next))
      if(Object.hasOwn(contents,key) && Object.hasOwn(owned,key) && contents[key]!==owned[key])
        throw Error('Refusing to overwrite manually edited language key: '+key);
    ownedJson[relative]=next;
    compiled.files[relative]=encode(description?{...existing,items:{...contents,...next}}:{...contents,...next});
  }
  for(const result of compiled.results.filter(r=>r.status==='skipped')) {
    const saved=previous.results.find(r=>r.id===result.id), old=saved?.status==='generated'?saved:saved?.lastSuccessful;
    if(old){
      result.lastSuccessful=old;
      for(const relative of old.artifacts??[])
        if(previous.files[relative])compiled.files[relative]=previous.files[relative];
    }
  }
  // A rejected edit must retain the entire last successful route, including removal
  // of its obsolete native alternatives. First-time failures protect old routes.
  const firstFailures=new Set(compiled.results.filter(r=>r.status==='skipped'&&!r.lastSuccessful)
    .flatMap(r=>(document.data.recipes[r.id]?.outputs??[])
      .map(v=>document.data.items[v.ref]?.itemId)));
  const managedOutputs=compiled.results.flatMap(r=>(r.status==='generated'?r:r.lastSuccessful)?.outputs??[])
      .filter(id=>!firstFailures.has(id));
  const nativeIds=Object.entries(resources).filter(([p,v])=>p.startsWith('data/')&&p.includes('/recipes/')
    && v?.type==='kaleidoscope_fragrantorchard:juicer'&&managedOutputs.includes(v.extract?.result??v.output?.item))
    .map(([p])=>p.replace(/^data\/([^/]+)\/recipes\/(.*)\.json$/,'$1:$2'));
  compiled.files['kubejs/server_scripts/icecore_recipe_manager.js']=removalScript(managedOutputs,nativeIds);
  const obsolete={
    'icecore_oven_test.js':'28725d565bcb979bc31b84365c5260ddfd74cea3ede11ed1f0ec038994732a4a',
    'icecore_mixing_bowl_test.js':'aac28d8967b90b7e2f332e3198ffcfe734e800071db87a64bdbc93c4d0ce0918',
    'add_cuisine_recipes.js':'fe532589f4c41ba30c02a5c70cb852de831aedf8e09b90248907c40e14d5e5b7',
    'remove_recipes.js':'3effa2411cf73bb908c653f3f67d80e6cafcfa769194757d44a29a7f9f8c68ef',
  };
  const cleanup=new Set();
  for(const [name,hash]of Object.entries(obsolete)){
    const relative='kubejs/server_scripts/'+name, target=path.join(cfg.client,relative);
    if(fs.existsSync(target)) {
      if(digest(fs.readFileSync(target))!==hash)throw Error('Legacy script changed; refusing cleanup: '+relative);
      cleanup.add(relative);
    }
  }
  const changes=[];
  for(const relative of new Set([...Object.keys(previous.files),...Object.keys(compiled.files),...cleanup])){
    const target=path.resolve(cfg.client,relative);if(!target.startsWith(path.resolve(cfg.client)+path.sep))throw Error('Unsafe path');
    const before=fs.existsSync(target)?fs.readFileSync(target,'utf8'):null, after=compiled.files[relative]??null;
    if(before!==after){
      if(!cleanup.has(relative) && !Object.hasOwn(ownedJson,relative) && before!==null
          && (previous.files[relative]===undefined || before!==previous.files[relative]))
        throw Error('Refusing to overwrite manually authored/edited file: '+relative);
      changes.push({relative,before,after});
    }
  }
  const report={sourceHash:digest(raw),jars:resources.provenance,generated:compiled.results.filter(r=>r.status==='generated').length,skipped:compiled.results.filter(r=>r.status==='skipped'),itemIssues:compiled.itemIssues,tagIssues:compiled.tagIssues,changes:changes.map(c=>c.relative)};
  if(command==='apply'){
    fs.mkdirSync(stateDir,{recursive:true});
    if(changes.length)fs.writeFileSync(path.join(stateDir,'rollback.json'),encode({manifest:previous,files:Object.fromEntries(changes.map(c=>[c.relative,c.before]))}));
    try{for(const c of changes){const target=path.join(cfg.client,c.relative);if(c.after===null)fs.rmSync(target,{force:true});else{fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,c.after);}}fs.writeFileSync(manifestPath,encode({...report,ownedJson,files:compiled.files,results:compiled.results}));}
    catch(error){
      for(const c of changes) {
        const target=path.join(cfg.client,c.relative);
        if(c.before===null)fs.rmSync(target,{force:true});
        else {fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,c.before);}
      }
      fs.writeFileSync(manifestPath,encode(previous));throw error;
    }
  }else if(!['check','diff'].includes(command))throw Error('Use check, diff, apply or rollback');
  if(command==='diff')report.details=changes;
  return report;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  try{const args=process.argv.slice(2),options={};for(let i=1;i<args.length;i+=2){if(!['--source','--client'].includes(args[i]))throw Error('Unknown option '+args[i]);options[args[i].slice(2)]=args[i+1];}process.stdout.write(encode(run(args[0]??'check',options)));}catch(error){process.stderr.write(error.message+'\n');process.exitCode=1;}
}
