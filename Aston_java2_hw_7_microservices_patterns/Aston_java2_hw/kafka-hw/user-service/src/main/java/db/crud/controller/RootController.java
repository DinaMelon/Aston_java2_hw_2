package db.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Точка входа в API (HATEOAS): клиенту достаточно знать один URL — {@code GET /api},
 * остальные адреса он берёт из ссылок.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "API root", description = "Точка входа HATEOAS: ссылки на доступные ресурсы")
public class RootController {

    @Operation(summary = "Точка входа в API",
            description = "Возвращает ссылки на доступные ресурсы (`users`) и документацию (`documentation`).")
    @GetMapping
    public RepresentationModel<?> root() {
        RepresentationModel<?> model = new RepresentationModel<>();
        model.add(linkTo(RootController.class).withSelfRel());
        model.add(linkTo(UserController.class).withRel("users"));
        model.add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/swagger-ui.html").toUriString(), "documentation"));
        return model;
    }
}
