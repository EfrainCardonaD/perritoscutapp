package com.cut.cardona.util;

import jakarta.persistence.EntityManager;

public class CreatePersistence {
    public static void main(String[] args) {


        EntityManager entityManager = JPAutil.getEntityManager();
        entityManager.close();


    }
}
