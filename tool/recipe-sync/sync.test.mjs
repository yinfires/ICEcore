import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {compile, price,run} from './sync.mjs';

test('nine seasoning inputs produce one bottle with sixteen native uses', () => {
  const items = Object.fromEntries([
    ['bowl','icecore:mixing_bowl'], ['a','test:a'], ['b','test:b'], ['c','test:c'],
    ['empty','kaleidoscope_grilling:empty_seasoning_bottle'],
    ['result','kaleidoscope_grilling:special_seasoning'],
  ].map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'seasoning',name:'特殊调料瓶',workstation:'bowl',
    inputs:['a','b','c'].map(ref=>({type:'item',ref,count:3})),
    attachments:[{type:'item',ref:'empty',count:1}],
    outputs:[{type:'item',ref:'result',count:16}]};
  const compiled=compile({data:{items,tags:{},recipes:{seasoning:recipe}}});
  assert.equal(compiled.results[0].status,'generated');
  const generated=JSON.parse(compiled.files['kubejs/data/icecore/recipes/recipe_manager/seasoning.json']);
  assert.equal(generated.ingredients.length,9);
  assert.deepEqual(generated.results,[{item:items.result.itemId,nbt:{SeasoningUses:0,
    SeasoningIngredients:['test:a','test:a','test:a','test:b','test:b','test:b','test:c','test:c','test:c']},
    carrier:{item:items.empty.itemId}}]);
  const names=JSON.parse(compiled.files['kubejs/assets/icecore/lang/zh_cn.json']);
  assert.equal(names['item.kaleidoscope_grilling.empty_seasoning_bottle'],'空调料瓶');
  assert.equal(Object.hasOwn(names,'item.kaleidoscope_grilling.special_seasoning'),false);
  recipe.inputs[0].count=4;
  const invalid=compile({data:{items,tags:{},recipes:{seasoning:recipe}}});
  assert.equal(invalid.results[0].status,'skipped');
  assert.equal(invalid.results[0].reason,'Mixing input capacity exceeded');
});

test('manual price wins and automatic price fills missing manual value',()=>{
  assert.equal(price({manualPrice:0,autoPrice:99}),0);
  assert.equal(price({autoPrice:4.5}),5);
  assert.throws(()=>price({autoPrice:-1}));
});

test('unknown tags are reported and never become displayed translation keys',()=>{
  const known='tag_1790130836441';
  const result=compile({data:{items:{fish:{id:'fish',itemId:'test:fish',name:'鱼',tags:[known,'missing']}},tags:{[known]:{id:known,name:'基础食材'}},recipes:{}}});
  assert.deepEqual(result.tagIssues,[{itemId:'test:fish',tag:'missing'}]);
  assert.deepEqual(JSON.parse(result.files['config/icecore/recipe_manager_items.json']).items['test:fish'].tags,[]);
  const names=JSON.parse(result.files['kubejs/assets/kaleidoscope_grilling/lang/zh_cn.json']);
  assert.equal(names['block.kaleidoscope_grilling.seasoning_bottle'],'空调料瓶');
  assert.equal(JSON.parse(result.files['kubejs/assets/kaleidoscope_fragrantorchard/lang/zh_cn.json'])['fluid.kaleidoscope_fragrantorchard.milk'],'牛奶');
  assert.equal(JSON.parse(result.files['kubejs/assets/kaleidoscope_cookery/lang/zh_cn.json'])['item.kaleidoscope_cookery.iron_kitchen_knife'],'菜刀');
});

test('juicer mixed ingredients expand to five single slots and reject overflow',()=>{
  const items=Object.fromEntries([['device','kaleidoscope_fragrantorchard:juicer'],['a','test:a'],['b','test:b'],['out','test:drink'],['cup','test:cup'],['water','minecraft:water_bucket']].map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'mix',workstation:'device',inputs:[{type:'item',ref:'a',count:2},{type:'item',ref:'b',count:3},{type:'item',ref:'water',count:1}],outputs:[{type:'item',ref:'out',count:1}],attachments:[{type:'item',ref:'cup',count:1}]};
  const compileRecipe=()=>compile({data:{items,tags:{},recipes:{mix:recipe}}});
  const json=JSON.parse(compileRecipe().files['kubejs/data/icecore/recipes/recipe_manager/mix.json']);
  assert.equal(json.mode,'mixed');assert.equal(json.input.items.length,5);
  assert.ok(json.input.items.every(i=>!i.count));
  recipe.inputs[0].count=3;
  assert.equal(compileRecipe().results[0].reason,'Juicer input capacity exceeded');
});

test('bucket juice uses native fluid filling and leaves cup extraction as a distinct recipe',()=>{
  const items=Object.fromEntries([['device','kaleidoscope_fragrantorchard:juicer'],['grape','kaleidoscope_tavern:grape'],['bucket','kaleidoscope_tavern:grape_bucket'],['empty','minecraft:bucket']].map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'bucket',workstation:'device',inputs:[{type:'item',ref:'grape',count:4}],outputs:[{type:'item',ref:'bucket',count:1}],attachments:[{type:'item',ref:'empty',count:1}]};
  const json=JSON.parse(compile({data:{items,tags:{},recipes:{bucket:recipe}}}).files['kubejs/data/icecore/recipes/recipe_manager/bucket.json']);
  assert.equal(json.mode,'juice');assert.equal(json.input.fluid,250);assert.equal(json.output.fluid,'kaleidoscope_tavern:grape_juice');assert.equal(json.extract,undefined);
  items.cup={id:'cup',itemId:'kaleidoscope_tavern:empty_glassware',name:'杯',tags:[]};
  items.drink={id:'drink',itemId:'kaleidoscope_fragrantorchard:grape_juice',name:'葡萄汁',tags:[]};
  const cup={...recipe,id:'cup',outputs:[{type:'item',ref:'drink',count:4}],attachments:[{type:'item',ref:'cup',count:1}]};
  const resources={'data/test/recipes/grape.json':{type:'kaleidoscope_fragrantorchard:juicer',mode:'juice',output:{fluid:'kaleidoscope_tavern:grape_juice'},extract:{result:items.drink.itemId,fluid:500}}};
  const compiled=compile({data:{items,tags:{},recipes:{bucket:recipe,cup}}},resources);
  const cupJson=JSON.parse(compiled.files['kubejs/data/icecore/recipes/recipe_manager/cup.json']);
  assert.equal(compiled.results.filter(r=>r.status==='generated').length,2);
  assert.equal(cupJson.input.fluid,250);assert.equal(cupJson.extract.fluid,250);
});

test('all native juice cups use 250mB while preserving manager batch quantities',()=>{
  for(const fruit of ['apple','watermelon','pear','peach','orange','grape']) {
    const items=Object.fromEntries([['device','kaleidoscope_fragrantorchard:juicer'],['fruit','test:'+fruit],['drink','kaleidoscope_fragrantorchard:'+fruit+'_juice'],['cup','test:cup']].map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
    const recipe={id:'juice',workstation:'device',inputs:[{type:'item',ref:'fruit',count:2}],outputs:[{type:'item',ref:'drink',count:4}],attachments:[{type:'item',ref:'cup',count:1}]};
    const resources={native:{type:'kaleidoscope_fragrantorchard:juicer',mode:'juice',output:{fluid:'test:'+fruit+'_juice'},extract:{result:items.drink.itemId,fluid:500}}};
    const json=JSON.parse(compile({data:{items,tags:{},recipes:{juice:recipe}}},resources).files['kubejs/data/icecore/recipes/recipe_manager/juice.json']);
    assert.equal(json.extract.fluid,250,fruit);assert.equal(json.input.fluid*2,1000,fruit);
  }
});

test('milk mixed juice requires the fluid actually injected by native milk buckets',()=>{
  const items=Object.fromEntries([['device','kaleidoscope_fragrantorchard:juicer'],['fruit','test:pomegranate'],['sugar','minecraft:sugar'],['milk','minecraft:milk_bucket'],['out','test:custard'],['cup','test:cup']].map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'custard',workstation:'device',inputs:[{type:'item',ref:'fruit',count:3},{type:'item',ref:'sugar',count:2},{type:'item',ref:'milk',count:1}],outputs:[{type:'item',ref:'out',count:4}],attachments:[{type:'item',ref:'cup',count:1}]};
  const json=JSON.parse(compile({data:{items,tags:{},recipes:{custard:recipe}}}).files['kubejs/data/icecore/recipes/recipe_manager/custard.json']);
  assert.equal(json.input.fluid,'kaleidoscope_fragrantorchard:milk_still');assert.equal(json.input.amount,1000);
  assert.equal(json.input.items.length,5);assert.equal(json.output.count,4);
});

test('stockpot outputs without an attachment disable the native default bowl',()=>{
  const items=Object.fromEntries([['device','kaleidoscope_cookery:stockpot'],['input','test:potato'],
    ['output','test:fries'],['fluid','kaleidoscope_grilling:canola_oil_bucket']]
    .map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'fries',workstation:'device',inputs:[{type:'item',ref:'input',count:1}],
    outputs:[{type:'item',ref:'output',count:1}],attachments:[{type:'item',ref:'fluid',count:1}]};
  const result=compile({data:{items,tags:{},recipes:{fries:recipe}}});
  const json=JSON.parse(result.files['kubejs/data/icecore/recipes/recipe_manager/fries.json']);
  assert.equal(json.empty_carrier,true);assert.equal(json.soup_base,'icecore:canola_oil');
});

test('juice buckets retain real fluid metadata and the vanilla bucket carrier',()=>{
  const items=Object.fromEntries([['device','kaleidoscope_fragrantorchard:juicer'],['input','test:grape'],
    ['output','kaleidoscope_tavern:grape_bucket'],['empty','minecraft:bucket']]
    .map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
  const recipe={id:'juice',workstation:'device',inputs:[{type:'item',ref:'input',count:1}],
    outputs:[{type:'item',ref:'output',count:1}],attachments:[{type:'item',ref:'empty',count:1}]};
  const result=compile({data:{items,tags:{},recipes:{juice:recipe}}});
  assert.deepEqual(JSON.parse(result.files['kubejs/data/icecore/icecore/container_compat/recipe_manager/kaleidoscope_tavern/grape_bucket.json']),
    {filled:items.output.itemId,empty:'minecraft:bucket',category:'bucket',behavior:'fluid',fluid:'kaleidoscope_tavern:grape_juice',amount:1000});
});

test('invalid food prices preserve the last successful menu and metadata',()=>{
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'icecore-recipe-price-'));
  try {
    const client=path.join(root,'client'),source=path.join(root,'source.json');
    const item={id:'food',itemId:'test:food',name:'测试菜',tags:['tag_1790175849511','tag_food_main_staple'],autoPrice:6};
    const document={data:{items:{food:item},tags:Object.fromEntries(item.tags.map(id=>[id,{id,name:id}])),recipes:{}}};
    const save=()=>fs.writeFileSync(source,JSON.stringify(document));save();run('apply',{client,source});
    item.autoPrice=-1;save();const report=run('apply',{client,source});
    assert.equal(report.itemIssues.length,1);
    assert.equal(JSON.parse(fs.readFileSync(path.join(client,'kubejs/data/cozycafe/menu/test/food.json'),'utf8')).price,6);
    assert.equal(JSON.parse(fs.readFileSync(path.join(client,'config/icecore/recipe_manager_items.json'),'utf8')).items['test:food'].price,6);
    assert.equal(run('diff',{client,source}).changes.length,0);
  } finally {fs.rmSync(root,{recursive:true,force:true});}
});

test('rejected edits retain successful recipes, carriers and native-route removal',()=>{
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'icecore-recipe-retain-'));
  try {
    const client=path.join(root,'client'), source=path.join(root,'source.json');
    const items=Object.fromEntries([['device','icecore:mixing_bowl'],['input','test:input'],
      ['output','test:output'],['carrier','minecraft:bowl']]
      .map(([id,itemId])=>[id,{id,itemId,name:id,tags:[]}]));
    const recipe={id:'retained',workstation:'device',inputs:[{type:'item',ref:'input',count:1}],
      outputs:[{type:'item',ref:'output',count:1}],attachments:[{type:'item',ref:'carrier',count:1}]};
    const document={data:{items,tags:{},recipes:{retained:recipe}}};
    const save=()=>fs.writeFileSync(source,JSON.stringify(document));save();
    run('apply',{client,source});
    const target=path.join(client,'kubejs/data/icecore/recipes/recipe_manager/retained.json');
    const successful=fs.readFileSync(target,'utf8');
    recipe.inputs[0].count=10;save();
    const rejected=run('apply',{client,source});assert.equal(rejected.skipped.length,1);
    assert.equal(fs.readFileSync(target,'utf8'),successful);
    assert.equal(fs.existsSync(path.join(client,'kubejs/data/icecore/icecore/container_compat/recipe_manager/test/output.json')),true);
    assert.match(fs.readFileSync(path.join(client,'kubejs/server_scripts/icecore_recipe_manager.js'),'utf8'),/test:output/);
    assert.equal(run('diff',{client,source}).changes.length,0);
    const firstClient=path.join(root,'first-client');
    run('apply',{client:firstClient,source});
    assert.equal(fs.existsSync(path.join(firstClient,'kubejs/data/icecore/recipes/recipe_manager/retained.json')),false);
    assert.doesNotMatch(fs.readFileSync(path.join(firstClient,'kubejs/server_scripts/icecore_recipe_manager.js'),'utf8'),/test:output/);
  } finally {fs.rmSync(root,{recursive:true,force:true});}
});

test('apply is idempotent, preserves manual language keys, cleans renamed keys and rolls back',()=>{
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'icecore-recipe-sync-'));
  try {
    const client=path.join(root,'client'), source=path.join(root,'source.json');
    const lang=path.join(client,'kubejs/assets/icecore/lang/zh_cn.json');
    fs.mkdirSync(path.dirname(lang),{recursive:true});
    fs.writeFileSync(lang,JSON.stringify({'manual.key':'保留'}));
    const data={items:{a:{id:'a',itemId:'test:before',name:'旧名',tags:[]}},tags:{},recipes:{}};
    const save=()=>fs.writeFileSync(source,JSON.stringify({data}));save();
    run('apply',{client,source});assert.equal(run('diff',{client,source}).changes.length,0);
    data.items.a.itemId='test:after';data.items.a.name='新名';save();
    run('apply',{client,source});
    let names=JSON.parse(fs.readFileSync(lang,'utf8'));
    assert.equal(names['manual.key'],'保留');assert.equal(names['item.test.before'],undefined);
    assert.equal(names['item.test.after'],'新名');
    run('apply',{client,source}); // A no-op must not destroy the last useful backup.
    run('rollback',{client,source});names=JSON.parse(fs.readFileSync(lang,'utf8'));
    assert.equal(names['manual.key'],'保留');assert.equal(names['item.test.before'],'旧名');
    assert.equal(names['item.test.after'],undefined);
    names['item.test.before']='手工修改';fs.writeFileSync(lang,JSON.stringify(names));
    data.items.a.itemId='test:before';save();
    assert.throws(()=>run('apply',{client,source}),/manually edited language key/);
  } finally {fs.rmSync(root,{recursive:true,force:true});}
});

test('legacy recipe scripts are backed up, deleted and restored without touching utility scripts',()=>{
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'icecore-legacy-'));
  try {
    const client=path.join(root,'client'),source=path.join(root,'source.json'),scripts=path.join(client,'kubejs/server_scripts');
    fs.mkdirSync(scripts,{recursive:true});
    const old="// 移除原始合成台配方\r\nServerEvents.recipes(event => {\r\n    // 移除水产乐事炸鱼卷的原始无序合成配方\r\n    event.remove({ id: 'aquaculturedelight:fried_perch_roll' })\r\n})\r\n";
    fs.writeFileSync(path.join(scripts,'remove_recipes.js'),old.replaceAll('\r\n','\n'));
    fs.writeFileSync(path.join(scripts,'handid.js'),'utility');
    fs.writeFileSync(source,JSON.stringify({data:{items:{},tags:{},recipes:{}}}));
    assert.ok(run('diff',{client,source}).changes.includes('kubejs/server_scripts/remove_recipes.js'));
    run('apply',{client,source});assert.equal(fs.existsSync(path.join(scripts,'remove_recipes.js')),false);
    assert.equal(fs.readFileSync(path.join(scripts,'handid.js'),'utf8'),'utility');
    run('rollback',{client,source});assert.equal(fs.readFileSync(path.join(scripts,'remove_recipes.js'),'utf8'),old.replaceAll('\r\n','\n'));
    fs.appendFileSync(path.join(scripts,'remove_recipes.js'),'// manual');
    assert.throws(()=>run('apply',{client,source}),/Legacy script changed/);
  } finally {fs.rmSync(root,{recursive:true,force:true});}
});
