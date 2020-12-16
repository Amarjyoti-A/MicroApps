package com.efx.gcs.messagedelivery.controller;

import com.efx.gcs.messagedelivery.TemplatePersistenceService;
import com.efx.gcs.messagedelivery.domain.ApiError;
import com.efx.gcs.messagedelivery.domain.PersistenceServiceHealthResponse;
import com.efx.gcs.messagedelivery.domain.exception.DuplicateTemplateException;
import com.efx.gcs.messagedelivery.domain.exception.InvalidMessageRequestException;
import com.efx.gcs.messagedelivery.domain.exception.InvalidTemplateContentException;
import com.efx.gcs.messagedelivery.domain.exception.TemplateNotFoundException;
import com.efx.gcs.messagedelivery.domain.template.CreateMessageTemplateRequest;
import com.efx.gcs.messagedelivery.domain.template.DeleteTemplateResponse;
import com.efx.gcs.messagedelivery.domain.template.MessageTemplate;
import com.efx.gcs.messagedelivery.domain.template.MessageTemplateContent;
import com.efx.gcs.messageworker.domain.exception.InvalidTemplateException;
import com.efx.gcs.messageworker.renderer.TemplateRenderer;
import com.efx.pet.utility.logging.PetLogger;
import com.efx.pet.utility.logging.PetLoggerFactory;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@RestController
@RequestMapping(path = "/template")
@ApiResponses(value = {
    @ApiResponse(code = 200, message = "Success"),
    @ApiResponse(code = 422, message = "Client supplied improper or invalid information in the request", response = ApiError.class),
    @ApiResponse(code = 401, message = "Unauthorized", response = ApiError.class),
    @ApiResponse(code = 403, message = "Forbidden", response = ApiError.class),
    @ApiResponse(code = 404, message = "Path or resource Not Found", response = ApiError.class),
    @ApiResponse(code = 500, message = "Internal server error occurred", response = ApiError.class),
    @ApiResponse(code = 503, message = "Service unavailable", response = ApiError.class)
})
public class TemplateController {

    PetLogger LOGGER = PetLoggerFactory.getLogger(TemplateController.class);

    private TemplatePersistenceService templatePersistenceService;

    private TemplateRenderer templateRenderer;

    @Autowired
    public TemplateController(TemplatePersistenceService templatePersistenceService, TemplateRenderer templateRenderer) {
        this.templatePersistenceService = templatePersistenceService;
        this.templateRenderer = templateRenderer;
    }

    public void setTemplatePersistenceService(TemplatePersistenceService templatePersistenceService) {
        this.templatePersistenceService = templatePersistenceService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields();
    }

    @GetMapping("/list")
    public List<MessageTemplate> listTemplates(){
        return templatePersistenceService.listTemplates();
    }

    @GetMapping("/{templateName}/{templateVersion}")
    public MessageTemplateContent getTemplate(@PathVariable String templateName, @PathVariable String templateVersion) throws TemplateNotFoundException {
        return templatePersistenceService.getTemplate(templateName, templateVersion);
    }

    @PostMapping("/")
    public MessageTemplate createTemplate(@RequestBody CreateMessageTemplateRequest createMessageTemplateRequest) throws InvalidMessageRequestException {
        validateCreateTemplateRequest(createMessageTemplateRequest);
        return templatePersistenceService.createTemplate(createMessageTemplateRequest.getTemplate(), createMessageTemplateRequest.getContent());
    }

    @ApiIgnore
    @DeleteMapping("/{templateName}/{templateVersion}")
    public DeleteTemplateResponse deleteTemplate(@PathVariable String templateName, @PathVariable String templateVersion) throws TemplateNotFoundException {
        return templatePersistenceService.deleteTemplate(templateName, templateVersion);
    }

    @GetMapping("/healthCheck")
    public PersistenceServiceHealthResponse getServiceHealth() {
        return templatePersistenceService.getServiceHealth();
    }

    @ApiIgnore
    @PutMapping("/{templateName}/{templateVersion}")
    public MessageTemplate updateTemplate(@PathVariable String templateName,
                                          @PathVariable String templateVersion,
                                          @RequestBody MessageTemplateContent templateContent) throws InvalidMessageRequestException {
        return templatePersistenceService.updateTemplate(templateName, templateVersion, templateContent);
    }

    enum TemplatePart { TEXT, SUBJECT, HTML };

    @ExceptionHandler(TemplateNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ApiError handleTemplateNotFound(TemplateNotFoundException e) {
        return new ApiError(e.getMessage(), HttpStatus.NOT_FOUND.toString());
    }

    @ExceptionHandler(DuplicateTemplateException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ApiError handleDuplicateTemplate(DuplicateTemplateException e) {
        return new ApiError(e.getMessage(), HttpStatus.CONFLICT.toString());
    }

    @ExceptionHandler(InvalidTemplateContentException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public ApiError handleInvalidTemplateContentException(InvalidTemplateContentException e){
        return new ApiError(e.getMessage(), HttpStatus.NOT_ACCEPTABLE.toString());
    }

    @ExceptionHandler(InvalidTemplateException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity handleInvalidTemplateContentException(InvalidTemplateException e){
        return ResponseEntity.unprocessableEntity().body(e.getMessage());
    }

    protected void validateCreateTemplateRequest(CreateMessageTemplateRequest request) throws InvalidMessageRequestException {
        if (isNull(request)) {
            throw new InvalidMessageRequestException("Template Request is null");
        } else if (isNull(request.getTemplate())) {
            throw new InvalidMessageRequestException("Message Template is null");
        } else if (isNull(request.getContent())) {
            throw new InvalidMessageRequestException("Template Content Object is null");
        } else if (isEmpty(request.getContent().getHtmlContent()) || isEmpty(request.getContent().getTextContent())) {
            throw new InvalidMessageRequestException("Template Html or text content is empty");
        } else if (isNull(request.getTemplate().getTemplateName())) {
            throw new InvalidMessageRequestException("Template Name is null");
        } else if (isNull(request.getTemplate().getVersion())) {
            throw new InvalidMessageRequestException("Template Version is null");
        } else {
            LOGGER.debug("Template validation complete");
        }
    }
}
