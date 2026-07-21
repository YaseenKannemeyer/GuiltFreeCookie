package za.ac.cput.guiltfreecookie.service;

import java.util.List;

public interface IService<T, ID> {

    T create(T t);

    T read(ID id);

    T update(T t);

    boolean delete(ID id);

}
