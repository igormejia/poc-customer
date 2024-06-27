package com.banorte;

import com.banorte.entity.Customer;
import com.banorte.entity.Product;
import com.banorte.repository.CustomerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Path("/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerAPI {

    private CustomerRepository pr;
    private Vertx vertx;
    private WebClient webClient;

    @PostConstruct
    void init(){
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8081)
                .setSsl(false)
                .setTrustAll(true);

        this.webClient = WebClient.create(vertx, options);
    }


    @Inject
    public CustomerAPI(CustomerRepository pr,Vertx vertx){
        this.pr = pr;
        this.vertx = vertx;
    }

    @GET
    @Blocking
    public Multi<Customer> list() {
        return Multi.createFrom().iterable(pr.listCustomer());
    }


    @GET
    @Path("/{id}")
    public Uni<Customer> getById(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> pr.findCustomer(id));
    }

    @GET
    @Path("/{id}/product")
    @Blocking
    public Uni<Customer> getByIdProduct(@PathParam("id") Long id) {
        Uni<Customer> customerUni = getById(id);
        Uni<List<Product>> productsUni = getAllProducts();

        return Uni.combine().all().unis(customerUni, productsUni)
                .asTuple()
                .onItem()
                .transformToUni(tuple -> {
                    Customer customer = tuple.getItem1();
                    List<Product> products = tuple.getItem2();

                    customer.getProducts().forEach(product -> {
                        products.forEach(productInner -> {
                            if (product.getId().equals(productInner.getId())) {
                                product.setName(productInner.getName());
                                product.setCode(productInner.getCode());
                                product.setDescription(productInner.getDescription());
                            }
                        });
                    });

                    return Uni.createFrom().item(customer);
                });
    }

    @POST
    @Blocking
    public Uni<Response> add(Customer c) {
        c.getProducts().forEach(p -> p.setCustomer(c));
        pr.createdCustomer(c);
        return Uni.createFrom().item(Response.ok().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@QueryParam("id") Long id) {
        Customer customer = pr.findCustomer(id);
        pr.deleteCustomer(customer);
        return Uni.createFrom().item(Response.ok().build());
    }
    @PUT
    public Response update(Customer p) {
        Customer customer = pr.findCustomer(p.getId());
        customer.setCode(p.getCode());
        customer.setAccountNumber(p.getAccountNumber());
        customer.setSurname(p.getSurname());
        customer.setPhone(p.getPhone());
        customer.setAddress(p.getAddress());
        customer.setProducts(p.getProducts());
        pr.updateCustomer(customer);
        return Response.ok().build();
    }

    private Uni<List<Product>> getAllProducts() {
        return webClient
                .get("/product")
                .send()
                .onFailure()
                .invoke(error -> log.error("Error al recuperar producto", error))
                .onItem()
                .transformToUni(exito -> {
                    List<Product> productsList = new ArrayList<>();
                    JsonArray listaJson = exito.bodyAsJsonArray();
                    listaJson.forEach(prooducto -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Product product = null;
                        try {
                            product = objectMapper.readValue(prooducto.toString(), Product.class);
                        } catch (JsonProcessingException e) {
                            log.error("Error al convertir el producto", e);
                        }
                        productsList.add(product);
                    });
                    return Uni.createFrom().item(productsList);
                });
    }
}
