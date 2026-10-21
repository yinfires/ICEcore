package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.regex.Pattern;

/** JEI-only recipe annotations; dynamic item descriptions and world progress remain intact. */
public final class JeiRecipeDetails {
    private static final Pattern TIME=Pattern.compile("^(制作时间|冷冻时间|烹饪时间|加工时间|发酵时间|酿造时间|Crafting Time|Freezing Time|Cooking Time|Processing Time|Fermentation Time)[:：].*",Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE=Pattern.compile("§.");
    public static boolean hidden(FormattedText text) {
        if(text==null)return false;
        if(text instanceof Component c && c.getContents() instanceof TranslatableContents t) {
            String key=t.getKey();
            if(key.equals("jei.tooltip.recipe.by"))return true;
            if(key.startsWith("jei.")&&(key.endsWith(".time")||key.endsWith(".cook_time")||key.endsWith(".cooking_time")||key.endsWith(".crafting_time")||key.endsWith(".freeze_time")))return true;
        }
        return TIME.matcher(STYLE.matcher(text.getString()).replaceAll("").strip()).matches();
    }
    private JeiRecipeDetails() {}
}
