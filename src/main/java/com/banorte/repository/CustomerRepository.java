package com.banorte.repository;

import com.banorte.entity.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CustomerRepository {

    @Inject
    EntityManager em;

    @Transactional
    public void createdCustomer(Customer p){
        em.persist(p);
    }

    @Transactional
    public void deleteCustomer(Customer p){
        em.remove(em.contains(p) ? p : em.merge(p));
    }

    @Transactional
    public List<Customer> listCustomer(){
        List<Customer> customers = em.createQuery("select p from Customer p").getResultList();
        return customers;
    }
    @Transactional
    public Customer findCustomer(Long Id){
        return em.find(Customer.class, Id);
    }
    @Transactional
    public void updateCustomer(Customer p){
        em.merge(p);
    }
}
