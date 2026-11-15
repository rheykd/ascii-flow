package ru.ascii.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

final class PresetStore {
    private final SharedPreferences storage;
    private final Settings settings;
    static final String[] KEYS={"mode","color","columns","glyph_scale","row_spacing","object_scale","font","cube_style","speed","brightness",
        "rotation_x","rotation_y","wave_frequency","wave_strength","torus_thickness","sphere_pulse","galaxy_arms","galaxy_rotation",
        "rain_length","rain_spacing","ripple_speed","ripple_life","media_contrast"};
    static final int[] MIN={0,0,24,40,70,40,0,0,0,5,0,0,25,10,30,0,1,0,25,1,25,1,25};
    static final int[] MAX={11,5,180,150,180,140,3,1,180,100,200,200,250,200,160,250,8,200,250,6,250,10,250};
    static final String[] BOOL={"eco","touch","media_invert","lock_clock","stars_animation","galaxy_spin","blackhole_waves"};
    PresetStore(Context c, Settings s) { settings=s; storage=c.getSharedPreferences("saved_presets",Context.MODE_PRIVATE); }
    static String cleanSymbols(String value) {
        if(value.length()>94)throw new IllegalArgumentException("Не больше 94 ASCII-символов");
        for(int i=0;i<value.length();i++)if(value.charAt(i)<32 || value.charAt(i)>126)
            throw new IllegalArgumentException("Используйте ASCII: латиницу, цифры и знаки");
        if(!value.isEmpty() && value.trim().isEmpty())throw new IllegalArgumentException("Добавьте хотя бы один видимый символ");
        return value;
    }
    JSONObject snapshot(boolean portable) throws JSONException {
        JSONObject result=new JSONObject(); Map<String,?> all=settings.prefs.getAll();
        for(String key:KEYS)if(all.containsKey(key))result.put(key,all.get(key));
        for(String key:BOOL)if(all.containsKey(key))result.put(key,all.get(key));
        result.put("symbols",settings.symbols());
        if(!portable)for(String key:new String[]{"photo_uri","video_uri"})if(all.containsKey(key))result.put(key,all.get(key));
        return result;
    }
    String[] names() { TreeSet<String> names=new TreeSet<>(storage.getAll().keySet()); return names.toArray(new String[0]); }
    void save(String name) throws JSONException {
        name=name.trim(); if(name.isEmpty()||name.length()>50)throw new IllegalArgumentException("Название: от 1 до 50 символов");
        if(!storage.contains(name)&&storage.getAll().size()>=50)throw new IllegalArgumentException("Максимум 50 пресетов");
        storage.edit().putString(name,snapshot(false).toString()).apply();
    }
    void remove(String name) { storage.edit().remove(name).apply(); }
    void apply(String name) throws JSONException {
        JSONObject values=validate(new JSONObject(storage.getString(name,"{}")));
        SharedPreferences.Editor edit=settings.prefs.edit();
        for(String key:KEYS)edit.remove(key);
        for(String key:BOOL)edit.remove(key);
        edit.remove("symbols");
        edit.remove("photo_uri"); edit.remove("video_uri");
        Iterator<String> keys=values.keys();
        while(keys.hasNext()) { String key=keys.next(); Object v=values.get(key);
            if(v instanceof Boolean)edit.putBoolean(key,(Boolean)v); else if(v instanceof Number)edit.putInt(key,((Number)v).intValue()); else edit.putString(key,(String)v);
        }
        // Local presets retain selected document URIs. Portable presets never contain these.
        JSONObject local=new JSONObject(storage.getString(name,"{}"));
        for(String key:new String[]{"photo_uri","video_uri"})if(local.has(key))edit.putString(key,local.getString(key));
        edit.apply();
    }
    static JSONObject validate(JSONObject input) throws JSONException {
        JSONObject result=new JSONObject();
        for(int i=0;i<KEYS.length;i++)if(input.has(KEYS[i])) {
            Object v=input.get(KEYS[i]); if(!(v instanceof Number))throw new IllegalArgumentException("Некорректный параметр: "+KEYS[i]);
            double n=((Number)v).doubleValue();
            if(!Double.isFinite(n)||n!=Math.rint(n)||n<MIN[i]||n>MAX[i])throw new IllegalArgumentException("Параметр вне диапазона: "+KEYS[i]);
            result.put(KEYS[i],(int)n);
        }
        for(String key:BOOL)if(input.has(key)) { if(!(input.get(key) instanceof Boolean))throw new IllegalArgumentException("Некорректный переключатель"); result.put(key,input.getBoolean(key)); }
        if(input.has("symbols")) { if(!(input.get("symbols") instanceof String))throw new IllegalArgumentException("Некорректные символы"); result.put("symbols",cleanSymbols(input.getString("symbols"))); }
        return result;
    }
    String exportAll() throws JSONException {
        JSONArray array=new JSONArray();
        for(String name:names())array.put(new JSONObject().put("name",name).put("settings",validate(new JSONObject(storage.getString(name,"{}")))));
        if(array.length()==0)array.put(new JSONObject().put("name","Текущие настройки").put("settings",snapshot(true)));
        return new JSONObject().put("format","ascii-flow-presets").put("version",1).put("presets",array).toString(2);
    }
    int importAll(String json) throws JSONException {
        JSONObject root=new JSONObject(json);
        if(!"ascii-flow-presets".equals(root.optString("format"))||root.optInt("version")!=1)throw new IllegalArgumentException("Неизвестный формат пресетов");
        JSONArray array=root.getJSONArray("presets");
        if(array.length()<1||array.length()+storage.getAll().size()>50)throw new IllegalArgumentException("В приложении можно хранить до 50 пресетов");
        Map<String,String> staged=new LinkedHashMap<>(); Set<String> used=new HashSet<>(Arrays.asList(names()));
        for(int i=0;i<array.length();i++) {
            JSONObject item=array.getJSONObject(i); String name=item.getString("name").trim();
            if(name.isEmpty()||name.length()>50)throw new IllegalArgumentException("Некорректное название пресета");
            String unique=name; int suffix=2;
            while(used.contains(unique)) {
                String ending=" ("+(suffix++)+")";
                unique=name.substring(0,Math.min(name.length(),50-ending.length()))+ending;
            }
            used.add(unique); staged.put(unique,validate(item.getJSONObject("settings")).toString());
        }
        SharedPreferences.Editor edit=storage.edit(); for(Map.Entry<String,String> item:staged.entrySet())edit.putString(item.getKey(),item.getValue()); edit.apply();
        return staged.size();
    }
}
