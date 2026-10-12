package com.christian.medicos.mapper;

import com.christian.commons.dto.medicos.MedicoRequest;
import com.christian.commons.dto.medicos.MedicoResponse;
import com.christian.commons.enums.EspecialidadMedico;
import com.christian.commons.mapper.CommonMapper;
import com.christian.medicos.entity.Medico;
import org.springframework.stereotype.Component;

@Component
public class MedicoMapper implements CommonMapper<MedicoRequest, MedicoResponse, Medico> {

    @Override
    public Medico requestAEntidad(MedicoRequest request) {
        return request == null
                ? null
                : Medico.crear(
                        request.nombre(),
                        request.apellidoPaterno(),
                        request.apellidoMaterno(),
                        request.edad(),
                        request.email(),
                        request.telefono(),
                        request.cedulaProfesional());
    }

    public Medico requestAEntidad(MedicoRequest request, EspecialidadMedico especialidadMedico) {

        Medico medico = requestAEntidad(request);

        medico.actualizarEspecialidad(especialidadMedico);

        return medico;
    }

    @Override
    public MedicoResponse entidadAResponse(Medico entidad) {

        return entidad == null
                ? null
                : new MedicoResponse(
                    entidad.getId(),
                    String.join(" ",
                            entidad.getNombre(),
                            entidad.getApellidoPaterno(),
                            entidad.getApellidoMaterno()),
                    entidad.getEdad(),
                    entidad.getEmail(),
                    entidad.getTelefono(),
                    entidad.getCedulaProfesional(),
                    entidad.getEspecialidad().getDescripcion(),
                    entidad.getDisponibilidad().getDescripcion(),
                    entidad.getDisponibilidad().getCodigo());
    }
}
