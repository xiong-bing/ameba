package ameba;

import ameba.container.Container;
import ameba.core.Application;
import ameba.exception.AmebaException;
import ameba.i18n.Messages;
import ameba.util.AmebaInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>Ameba class.</p>
 *
 * @author icode
 */
public class Ameba {
    private static final Logger logger = LoggerFactory.getLogger(Ameba.class);
    private static Application app;
    private static Container container;

    private Ameba() {
    }

    /**
     * <p>getInjectionManager.</p>
     *
     * @return a InjectionManager object.
     */
    public static InjectionManager getInjectionManager() {
        return container.getInjectionManager();
    }

    /**
     * <p>Getter for the field <code>container</code>.</p>
     *
     * @return a {@link ameba.container.Container} object.
     * @since 0.1.6e
     */
    public static Container getContainer() {
        return container;
    }

    /**
     * <p>Getter for the field <code>app</code>.</p>
     *
     * @return a {@link ameba.core.Application} object.
     */
    public static Application getApp() {
        return app;
    }

    public static String getVersion() {
        return AmebaInfo.getVersion();
    }

    /**
     * <p>printInfo.</p>
     *
     * @since 0.1.6e
     */
    public static void printInfo() {
        logger.info(AmebaInfo.getBanner(), getVersion());
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(Ameba::shutdown, "AmebaShutdownHook"));

        List<String> list = Lists.newArrayList();

        String idCommand = "--#";

        int idArgLen = idCommand.length();

        for (String arg : args) {
            if (arg.startsWith(idCommand)) {
                String idConf = arg.substring(idArgLen);
                if (StringUtils.isNotBlank(idConf)) {
                    list.add(idConf);
                }
            }
        }

        try {
            bootstrap(list.toArray(new String[list.size()]));
        } catch (Throwable e) {
            logger.error(Messages.get("info.service.error.startup"), e);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                //no op
            }
            shutdown();
            System.exit(500);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            //no op
        }
    }

    /**
     * <p>bootstrap.</p>
     *
     * @param ids a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public static void bootstrap(String... ids) throws Exception {
        bootstrap(new Application(ids));
    }

    /**
     * <p>bootstrap.</p>
     *
     * @param application a {@link ameba.core.Application} object.
     * @throws java.lang.Exception if any.
     */
    public static synchronized void bootstrap(Application application) throws Exception {
        if (Ameba.container != null) {
            throw new AmebaException(Messages.get("info.service.start"));
        }

        app = application;
        container = Container.create(app);

        // run
        logger.info(Messages.get("info.service.start"));
        container.start();
    }

    /**
     * <p>shutdown.</p>
     */
    public static synchronized void shutdown() {
        logger.info(Messages.get("info.service.shutdown"));
        if (container != null)
            try {
                container.shutdown();
            } catch (Exception e) {
                logger.error(Messages.get("info.service.error.shutdown"), e);
            }
        logger.info(Messages.get("info.service.shutdown.done"));
    }
}
