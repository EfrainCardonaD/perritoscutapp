package com.cut.cardona.modelo.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {


    @Query("SELECT u FROM Usuario u WHERE u.userName = :username OR u.email = :email")
    Optional<Usuario> findByUserNameOrEmail(@Param("username") String username, @Param("email") String email);


    Usuario findByUserName(String username);

    Usuario findByEmail(String email);



}
