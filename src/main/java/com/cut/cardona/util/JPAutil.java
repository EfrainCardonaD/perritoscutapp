package com.cut.cardona.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAutil {
    public static EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory("cut");

        public static EntityManager getEntityManager(){
            return FACTORY.createEntityManager();
        }
}
