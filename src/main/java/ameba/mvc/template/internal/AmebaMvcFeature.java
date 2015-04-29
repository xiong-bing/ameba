package ameba.mvc.template.internal;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.mvc.MvcFeature;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>AmebaMvcFeature class.</p>
 *
 * @author 张立鑫 IntelligentCode
 * @since 2013-08-07
 */
public class AmebaMvcFeature implements Feature {

    private static final String TPL_CACHE = "template.caching";
    private static final String TPL_MODULE_DIR_PR = "template.directory.module.";


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean configure(final FeatureContext context) {
        if (!context.getConfiguration().isRegistered(MvcFeature.class)) {
            context.register(MvcFeature.class);
        }

        Map<String, String> tempConf = Maps.newHashMap();

        for (String key : context.getConfiguration().getPropertyNames()) {
            if (key.startsWith(TemplateUtils.TPL_ENGINE_DIR_PR)) {//模板引擎默认路径
                String engine = key.replaceFirst(Pattern.quote(
                        TemplateUtils.TPL_ENGINE_DIR_PR.substring(0, TemplateUtils.TPL_ENGINE_DIR_PR.length() - 1)
                ), "");
                String confKey = MvcFeature.TEMPLATE_BASE_PATH + engine;
                String value = (String) context.getConfiguration().getProperty(confKey);
                String append = (String) context.getConfiguration().getProperty(key);
                value = TemplateUtils.getTemplateEngineDirConfig(value, engine.substring(1), context, tempConf);
                if (StringUtils.isBlank(value)) {
                    value = append;
                } else {
                    value += "," + append;
                }
                tempConf.put(confKey, value);
            } else if (key.startsWith(TPL_MODULE_DIR_PR)) {//模块自定义模板路径
                String confKey = key.replaceFirst(Pattern.quote(TPL_MODULE_DIR_PR), "");
                int i = confKey.indexOf(".");
                if (i != -1) {
                    String engine = confKey.substring(0, i);
                    confKey = MvcFeature.TEMPLATE_BASE_PATH + "." + engine;
                    String value = (String) context.getConfiguration().getProperty(confKey);
                    String append = (String) context.getConfiguration().getProperty(key);
                    value = TemplateUtils.getTemplateEngineDirConfig(value, engine, context, tempConf);
                    if (StringUtils.isBlank(value)) {
                        value = append;
                    } else {
                        value += "," + append;
                    }
                    tempConf.put(confKey, value);
                }
            } else if (key.startsWith(TPL_CACHE + ".")) {
                tempConf.put(MvcFeature.CACHE_TEMPLATES + key.replaceFirst(Pattern.quote(TPL_CACHE), ""),
                        (String) context.getConfiguration().getProperty(key));
            }
        }

        for (String key : tempConf.keySet()) {
            context.property(key, tempConf.get(key));
        }

        context.property(MvcFeature.TEMPLATE_BASE_PATH,
                context.getConfiguration().getProperty(TemplateUtils.TPL_DIR));

        context.property(MvcFeature.CACHE_TEMPLATES,
                context.getConfiguration().getProperty(TPL_CACHE));

        return true;
    }

}
