package com.yinfires.icecore.compat.cozycafe.customer;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public record CafeCustomerConfig(List<Type> types, int minIntervalTicks, int maxIntervalTicks,
                                 double jitter, double vacancyBoost, double preferenceChance,
                                 double menuBoost, double reputationBoost, double tipChance, int timeoutPenalty) {
    public record Skin(String id,String resource,String file,boolean slim,double weight) {}
    public record Type(String id,String nameKey,int cap,double weight,List<String> likes,List<String> dislikes,List<Skin> skins) {}
    public static CafeCustomerConfig empty() { return new CafeCustomerConfig(List.of(),200,900,.2,.2,.9,.25,.2,.2,2); }
    public Type type(String id) { return types.stream().filter(t->t.id.equals(id)).findFirst().orElse(null); }
    public static CafeCustomerConfig read(Path path) throws java.io.IOException {
        if(!Files.exists(path)) throw new java.io.IOException("Missing cafe_customers.json");
        if(Files.size(path)>262144) throw new IllegalArgumentException("Customer config exceeds 256 KiB");
        JsonObject root=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray array=root.getAsJsonArray("customers");
        if(array==null || array.size()>128) throw new IllegalArgumentException("Expected at most 128 customers");
        List<Type> types=new ArrayList<>(); Set<String> ids=new HashSet<>();
        for(JsonElement e:array) {
            JsonObject o=e.getAsJsonObject(); String id=text(o,"id");
            if(!id.matches("[a-z0-9_]{1,64}") || !ids.add(id)) throw new IllegalArgumentException("Invalid/duplicate customer id: "+id);
            int cap=integer(o,"reputationCap",200);
            if(cap<1 || cap>1000000) throw new IllegalArgumentException("Invalid reputation cap");
            List<String> likes=tags(o,"likes"),dislikes=tags(o,"dislikes");
            if(likes.stream().anyMatch(dislikes::contains)) throw new IllegalArgumentException("Conflicting preferences: "+id);
            List<Skin> skins=new ArrayList<>(); Set<String> variants=new HashSet<>();
            if(o.has("skins")) for(JsonElement s:o.getAsJsonArray("skins")) {
                if(skins.size()>=32) throw new IllegalArgumentException("Too many skin variants");
                JsonObject v=s.getAsJsonObject(); String variant=text(v,"id");
                if(!variant.matches("[a-z0-9_]{1,64}") || !variants.add(variant)) throw new IllegalArgumentException("Invalid skin id");
                String resource=v.has("resource")?text(v,"resource"):"";
                String file=v.has("file")?text(v,"file"):"";
                if(!resource.isEmpty() && (ResourceLocation.tryParse(resource)==null||resource.contains("..")||resource.contains(":/"))) throw new IllegalArgumentException("Invalid skin resource");
                if(!file.isEmpty()) {
                    Path relative=Path.of(file);
                    if(relative.isAbsolute() || file.contains(":") || file.contains("\\") || Arrays.asList(file.split("/")).contains("..") || !file.endsWith(".png"))
                        throw new IllegalArgumentException("Unsafe skin path");
                    Path base=path.getParent().resolve("cafe_skins").toAbsolutePath().normalize();
                    Path resolved=base.resolve(relative).normalize();
                    if(Files.isSymbolicLink(base)||!resolved.startsWith(base) || Files.exists(resolved) && (!resolved.toRealPath().startsWith(base.toRealPath()) || Files.size(resolved)>65536))
                        throw new IllegalArgumentException("Unsafe/oversize skin file");
                }
                if(!resource.isEmpty() && !file.isEmpty()) throw new IllegalArgumentException("Choose resource or file");
                skins.add(new Skin(variant,resource,file,v.has("slim")&&v.get("slim").getAsBoolean(),positive(v,"weight",1)));
            }
            String nameKey=text(o,"nameKey");if(!nameKey.matches("[a-zA-Z0-9_.-]+"))throw new IllegalArgumentException("Invalid name translation key");
            types.add(new Type(id,nameKey,cap,positive(o,"weight",1),likes,dislikes,List.copyOf(skins)));
        }
        JsonObject settings=root.has("settings")?root.getAsJsonObject("settings"):new JsonObject();
        int min=(int)(positive(settings,"minIntervalSeconds",10)*20),max=(int)(positive(settings,"maxIntervalSeconds",45)*20);
        if(min<20 || max<min || max>72000) throw new IllegalArgumentException("Invalid spawn interval");
        int penalty=integer(settings,"timeoutPenalty",2);if(penalty<0||penalty>1000000)throw new IllegalArgumentException("Invalid timeout penalty");
        return new CafeCustomerConfig(List.copyOf(types),min,max,probability(settings,"jitter",.2),probability(settings,"vacancyBoost",.2),
                probability(settings,"preferenceChance",.9),probability(settings,"menuBoost",.25),probability(settings,"reputationBoost",.2),probability(settings,"tipChance",.2),penalty);
    }
    private static int integer(JsonObject o,String key,int fallback){
        if(!o.has(key))return fallback;
        try{return o.get(key).getAsBigDecimal().intValueExact();}catch(ArithmeticException invalid){throw new IllegalArgumentException("Invalid integer "+key,invalid);}
    }
    private static String text(JsonObject o,String key) {
        String value=o.get(key).getAsString();
        if(value.isBlank() || value.length()>256) throw new IllegalArgumentException("Invalid "+key);
        return value;
    }
    private static List<String> tags(JsonObject o,String key) {
        Set<String> result=new LinkedHashSet<>();
        if(o.has(key)) for(JsonElement e:o.getAsJsonArray(key)) {
            String id=e.getAsString();
            if(id.length()>256||!id.startsWith("icecore:recipe_manager/")||ResourceLocation.tryParse(id)==null) throw new IllegalArgumentException("Invalid recipe-manager preference tag");
            result.add(id);
            if(result.size()>64)throw new IllegalArgumentException("Too many preferences");
        }
        return List.copyOf(result);
    }
    private static double positive(JsonObject o,String key,double fallback) {
        double n=o.has(key)?o.get(key).getAsDouble():fallback;
        if(!Double.isFinite(n) || n<=0 || n>1000000) throw new IllegalArgumentException("Invalid "+key);
        return n;
    }
    private static double probability(JsonObject o,String key,double fallback) {
        double n=o.has(key)?o.get(key).getAsDouble():fallback;
        if(!Double.isFinite(n) || n<0 || n>1) throw new IllegalArgumentException("Invalid "+key);
        return n;
    }
}
