package com.christian.auth.services;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.christian.auth.dto.UsuarioUpdateRequest;
import com.christian.auth.exceptions.ConflictoException;
import com.christian.auth.exceptions.DatoInvalidoException;
import com.christian.auth.exceptions.RecursoNoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.christian.auth.dto.UsuarioRequest;
import com.christian.auth.dto.UsuarioResponse;
import com.christian.auth.entities.Rol;
import com.christian.auth.entities.Usuario;
import com.christian.auth.mapper.UsuarioMapper;
import com.christian.auth.repositories.RolRepository;
import com.christian.auth.repositories.UsuarioRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder; // bean BCrypt de SecurityConfig

    @Override
    @Transactional(readOnly = true) // solo lectura: más ligero, no abre escritura
    public Set<UsuarioResponse> listar() {
        log.info("Listado de todos los usuarios solicitado");
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::entityToResponse).collect(Collectors.toSet());
    }

    @Override
    public UsuarioResponse registrar(UsuarioRequest request) {

        String username = request.username().trim();

        log.info("Buscando usuario {}", username);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictoException("El usuario " + username + " ya está registrado");
        }

        Set<Rol> roles = resolverRoles(request.roles());

        Usuario usuario = usuarioMapper.requestToEntity(request,
                passwordEncoder.encode(request.password()), roles);

        usuarioRepository.save(usuario);
        return usuarioMapper.entityToResponse(usuario);
    }

    @Override
    public UsuarioResponse actualizar(String username, UsuarioUpdateRequest request) {

        log.info("Actualizando usuario {}", username);

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario: " + username));

        usuario.setRoles(resolverRoles(request.roles()));

        if (request.password() != null) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.entityToResponse(usuario);
    }

    @Override
    public void eliminar(String username) {
        log.info("Eliminando usuario {}", username);
        if (!usuarioRepository.existsByUsername(username)) {
            throw new RecursoNoEncontradoException("No se encontró el usuario: " + username);
        }
        usuarioRepository.deleteByUsername(username);
    }
    private Set<Rol> resolverRoles(Set<String> nombres) {
        return nombres.stream().map(nombre ->
                rolRepository.findByNombre(nombre).orElseThrow(() ->
                        new DatoInvalidoException("El rol " + nombre + " no existe"))
        ).collect(Collectors.toSet());
    }
}