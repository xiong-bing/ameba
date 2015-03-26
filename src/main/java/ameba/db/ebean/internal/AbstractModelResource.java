package ameba.db.ebean.internal;

import ameba.core.ws.rs.PATCH;
import ameba.db.model.Model;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Query;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @author icode
 */
public abstract class AbstractModelResource<T extends Model> {

    protected Class<T> modelType;
    protected final SpiEbeanServer server;
    protected String defaultFindOrderBy;
    @Context
    protected UriInfo uriInfo;

    public AbstractModelResource(Class<T> modelType) {
        this(modelType, (SpiEbeanServer) Ebean.getServer(null));
    }

    public AbstractModelResource(Class<T> modelType, SpiEbeanServer server) {
        this.modelType = modelType;
        this.server = server;
    }

    /**
     * Insert a model.
     *
     * @param model the model to insert
     */
    @POST
    public Response insert(@NotNull @Valid final T model) {
        BeanDescriptor descriptor = server.getBeanDescriptor(model.getClass());
        Object idProp = descriptor.getId((EntityBean) model);
        if (idProp instanceof CharSequence) {
            if (StringUtils.isNotBlank((CharSequence) idProp)) {
                descriptor.getIdProperty().setValue((EntityBean) model, null);
            }
        } else if (idProp != null) {
            descriptor.getIdProperty().setValue((EntityBean) model, null);
        }

        postInsertModel(model);
        server.save(model);
        Object id = server.getBeanId(model);

        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI createdUri = ub.path("" + id).build();

        return Response.created(createdUri).build();
    }

    protected void postInsertModel(final T model) {

    }


    /**
     * Update a model.
     *
     * @param id    the unique id of the model
     * @param model the model to update
     */
    @PUT
    @Path("{id}")
    public void update(@PathParam("id") String id, @NotNull @Valid final T model) {
        BeanDescriptor descriptor = server.getBeanDescriptor(model.getClass());
        descriptor.convertSetId(id, (EntityBean) model);
        EntityBeanIntercept intercept = ((EntityBean) model)._ebean_getIntercept();
        for (int i = 0; i < intercept.getPropertyLength(); i++) {
            intercept.markPropertyAsChanged(i);
        }
        postUpdateModel(model);
        server.update(model);
    }

    protected void postUpdateModel(final T model) {

    }

    /**
     * Update a model items.
     *
     * @param id    the unique id of the model
     * @param model the model to update
     */
    @PATCH
    @Path("{id}")
    public void patch(@PathParam("id") String id, @NotNull final T model) {
        BeanDescriptor descriptor = server.getBeanDescriptor(model.getClass());
        descriptor.convertSetId(id, (EntityBean) model);
        postPatchModel(model);
        server.update(model);
    }

    protected void postPatchModel(final T model) {

    }

    /**
     * Delete multiple model using Id's from the Matrix.
     *
     * @param ids The ids in the form "/resource/id1" or "/resource/id1;id2;id3"
     */
    @DELETE
    @Path("{ids}")
    public void deleteMultiple(@NotNull @PathParam("ids") final PathSegment ids) {
        String firstId = ids.getPath();
        Set<String> idSet = ids.getMatrixParameters().keySet();
        if (!idSet.isEmpty()) {
            Set<String> idCollection = Sets.newLinkedHashSet();
            idCollection.add(firstId);
            idCollection.addAll(idSet);
            postDeleteMultiple(idCollection);
            server.delete(modelType, idCollection);
        } else {
            postDelete(firstId);
            server.delete(modelType, firstId);
        }
    }

    protected void postDeleteMultiple(Set<String> idCollection) {

    }

    protected void postDelete(String id) {

    }

    /**
     * Configure the "Find By Id" query.
     * <p>
     * This is only used when no PathProperties where set via UriOptions.
     * </p>
     * <p>
     * This effectively controls the "default" query used to render this model.
     * </p>
     */
    protected void configDefaultFindByIdQuery(final Query<T> query) {

    }

    /**
     * Find a model given its Id.
     *
     * @param id the id of the model.
     */
    @GET
    @Path("{id}")
    public T find(@NotNull @PathParam("id") final String id) {
        Query<T> query = server.find(modelType);
        configDefaultFindByIdQuery(query);
        applyUriQuery(query);
        return query.setId(id).findUnique();
    }

    /**
     * Configure the "Find" query.
     * <p>
     * This is only used when no PathProperties where set via UriOptions.
     * </p>
     * <p>
     * This effectively controls the "default" query used with the find all
     * query.
     * </p>
     */
    protected void configDefaultFindQuery(final Query<T> query) {

    }


    /**
     * Find the beans for this beanType.
     * <p>
     * This can use URL query parameters such as order and maxrows to configure
     * the query.
     * </p>
     */
    @GET
    public List<T> find() {

        Query<T> query = server.find(modelType);

        configDefaultFindQuery(query);

        if (StringUtils.isNotBlank(defaultFindOrderBy)) {
            // see if we should use the default orderBy clause
            OrderBy<T> orderBy = query.orderBy();
            if (orderBy.isEmpty()) {
                query.orderBy(defaultFindOrderBy);
            }
        }

        applyUriQuery(query);

        return query.findList();
    }

    protected void applyUriQuery(final Query<T> query) {
        EbeanModelWriter.applyUriQuery(uriInfo.getQueryParameters(), query);
    }

}