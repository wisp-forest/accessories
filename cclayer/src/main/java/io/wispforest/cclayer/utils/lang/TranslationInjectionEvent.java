package io.wispforest.cclayer.utils.lang;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;

import java.util.List;
import java.util.Map;

public class TranslationInjectionEvent {
    public static Event<LanguageInjection> AFTER_LANGUAGE_LOAD = EventFactory.createArrayBacked(LanguageInjection.class,
            (listeners) -> (helper) -> {
                for (var event : listeners) event.generateLanguageTranslations(helper);
            }
    );

    public interface LanguageInjection {
        void generateLanguageTranslations(TranslationMapHelper helper);
    }

    public final static class TranslationMapHelper {
        private final Map<String, String> immutableTranslationView;
        private final Map<String, String> rawTranslationData;

        private final List<LanguageInfo> loadingDefinitions;

        public TranslationMapHelper(Map<String, String> translationData){
            this.immutableTranslationView = ImmutableMap.copyOf(translationData);
            this.rawTranslationData = translationData;

            this.loadingDefinitions = List.copyOf(Minecraft.getInstance().getLanguageManager().getLanguages().values());
        }

        public List<LanguageInfo> getLangDefinitions(){
            return this.loadingDefinitions;
        }

        public void addRedirections(Map<String, Redirection> redirections){
            redirections.forEach((key, redirection) -> this.addTranslation(key, redirection.getRedirect(this.getTranslations())));
        }

        public void addTranslation(Map<String, String> translations){
            translations.forEach(this::addTranslation);
        }

        public boolean addTranslation(String key, String value){
            if(!getTranslations().containsKey(key)){
                this.rawTranslationData.put(key, value);

                return true;
            }

            return false;
        }

        public Map<String, String> getTranslations() {
            return this.immutableTranslationView;
        }
    }

    public interface Redirection {
        String getRedirect(Map<String, String> translations);
    }
}
