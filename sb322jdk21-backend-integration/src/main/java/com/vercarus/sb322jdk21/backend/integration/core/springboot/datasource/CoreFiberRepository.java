package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import java.util.List;

public interface CoreFiberRepository<T, ID> {
    T findById(ID id);

    List<T> findAll();

    List<T> findAllById(Iterable<ID> var1);

    <S extends T> List<S> saveAll(Iterable<S> var1);

    <T> T save(T var1);

    void deleteById(ID var1);

    void delete(T var1);

    void deleteAll(Iterable<? extends T> var1);
}
