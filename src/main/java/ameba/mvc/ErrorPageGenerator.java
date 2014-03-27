package ameba.mvc;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.DefaultErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.server.mvc.spi.AbstractTemplateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashMap;

/**
 * 错误处理页面配置
 * Created by icode on 14-3-25.
 */
@Singleton
public abstract class ErrorPageGenerator extends DefaultErrorPageGenerator implements ExceptionMapper<Exception> {
    private static final Logger logger = LoggerFactory.getLogger(ErrorPageGenerator.class);
    private static final HashMap<Integer, String> errorTemplateMap = Maps.newHashMap();
    private static String defaultErrorTemplate;

    private static AbstractTemplateProcessor templateProcessor;

    public static void setTemplateProcessor(AbstractTemplateProcessor templateProcessor) {
        ErrorPageGenerator.templateProcessor = templateProcessor;
    }

    public static AbstractTemplateProcessor getTemplateProcessor() {
        return templateProcessor;
    }

    public static void pushErrorMap(int status, String tpl) {
        errorTemplateMap.put(status, tpl);
    }

    public static void pushAllErrorMap(HashMap<Integer, String> map) {
        errorTemplateMap.putAll(map);
    }


    public static HashMap<Integer, String> getErrorTemplateMap() {
        return errorTemplateMap;
    }

    public static String getDefaultErrorTemplate() {
        return defaultErrorTemplate;
    }

    public static void setDefaultErrorTemplate(String template) {
        defaultErrorTemplate = template;
    }


    @Override
    public Response toResponse(Exception e) {
        int status = 500;
        if (e instanceof WebApplicationException) {
            WebApplicationException ex = (WebApplicationException) e;
            Response response = ex.getResponse();
            status = response.getStatus();
        }

        return Response.status(status)
                .entity(generate(Request.create(),
                        status,
                        e.getMessage(),
                        StringUtils.join(e.getStackTrace(), "\n"),
                        e
                )).build();
    }

    @Override
    public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
        return generate(request, null, status, reasonPhrase, description, exception);
    }

    public String generate(Request request, Response response, int status, String reasonPhrase, String description, Throwable exception) {
        String tplName = errorTemplateMap.get(status);
        if (StringUtils.isBlank(tplName)) {
            if (StringUtils.isBlank(defaultErrorTemplate)) {
                return super.generate(request, status, reasonPhrase, description, exception);
            } else {
                tplName = defaultErrorTemplate;
            }
        }

        return processTemplate(tplName, request,
                (response != null ? response.getHeaders() : new MultivaluedHashMap<String, Object>()),
                status, reasonPhrase, description, exception);
    }

    protected abstract String processTemplate(String tplName, Request request,
                                              MultivaluedMap<String, Object> httpHeaders,
                                              int status, String reasonPhrase,
                                              String description, Throwable exception);

    public static class Error {
        public Request request;
        public int status;
        public String reasonPhrase;
        public String description;
        public Throwable exception;

        public Error(Request request, int status, String reasonPhrase, String description, Throwable exception) {
            this.request = request;
            this.status = status;
            this.reasonPhrase = reasonPhrase;
            this.description = description;
            this.exception = exception;
        }
    }
}
