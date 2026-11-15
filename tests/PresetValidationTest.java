package ru.ascii.wallpaper;
import org.json.JSONObject;

public final class PresetValidationTest {
    interface Operation { void run() throws Exception; }
    static void rejected(Operation op) throws Exception { try {op.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid data accepted"); }
    public static void main(String[] args) throws Exception {
        JSONObject input=new JSONObject().put("mode",8).put("columns",120).put("symbols","01").put("touch",true).put("photo_uri","content://private");
        JSONObject output=PresetStore.validate(input);
        if(output.has("photo_uri")||output.getInt("mode")!=8||!output.getString("symbols").equals("01"))throw new AssertionError("Portable preset filtering");
        rejected(()->PresetStore.validate(new JSONObject().put("columns",99999)));
        rejected(()->PresetStore.validate(new JSONObject().put("mode",1.5)));
        rejected(()->PresetStore.validate(new JSONObject().put("touch","yes")));
        rejected(()->PresetStore.validate(new JSONObject().put("symbols",123)));
        rejected(()->PresetStore.cleanSymbols("Привет")); rejected(()->PresetStore.cleanSymbols("   "));
        rejected(()->PresetStore.cleanSymbols("a\nb"));
        PresetStore.cleanSymbols(""); PresetStore.cleanSymbols(" .:-=+*#%@");
        for(int i=0;i<PresetStore.KEYS.length;i++) {
            PresetStore.validate(new JSONObject().put(PresetStore.KEYS[i],PresetStore.MIN[i]));
            PresetStore.validate(new JSONObject().put(PresetStore.KEYS[i],PresetStore.MAX[i]));
        }
        System.out.println("PASS: preset ranges/types, portable URI stripping, ASCII alphabet validation.");
    }
}
