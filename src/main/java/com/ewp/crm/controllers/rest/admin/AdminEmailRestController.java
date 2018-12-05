package com.ewp.crm.controllers.rest.admin;

import com.ewp.crm.configs.ImageConfig;
import com.ewp.crm.exceptions.email.MessageTemplateException;
import com.ewp.crm.models.MessageTemplate;
import com.ewp.crm.models.User;
import com.ewp.crm.service.impl.MessageTemplateServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class AdminEmailRestController {

    private final MessageTemplateServiceImpl messageTemplateService;
    private final ImageConfig imageConfig;

    @Autowired
    public AdminEmailRestController(MessageTemplateServiceImpl MessageTemplateService,
                                    ImageConfig imageConfig) {
        this.messageTemplateService = MessageTemplateService;
        this.imageConfig = imageConfig;
    }

    @ResponseBody
    @GetMapping(value = "/admin/image/{file}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public byte[] getImage(@PathVariable("file") String file) throws IOException {
        Path fileLocation = Paths.get(imageConfig.getPathForImages() + file + ".png");
        return Files.readAllBytes(fileLocation);
    }


    @PostMapping(value = {"/admin/editMessageTemplate"})
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public HttpStatus editETemplate(@RequestParam("templateName") String templateName,
                                    @RequestParam("templateText") String templateText,
                                    @RequestParam String otherTemplateText) {
        //TODO Убрать хардкод
        String msgTemplateDefaultTextBody = "%bodyText%";
        if (templateText.contains(msgTemplateDefaultTextBody) ^ otherTemplateText.contains(msgTemplateDefaultTextBody)) {
            throw new MessageTemplateException(msgTemplateDefaultTextBody + "должен присутствовать или остутствовать на обоих типах сообщения");
        }

        MessageTemplate messageTemplate = messageTemplateService.getByName(templateName);

        if (templateText.length() == 0 || otherTemplateText.length() == 0) {
            throw new MessageTemplateException("Шаблон не должен быть пустым на обоих типах сообщения");
        }

        if (messageTemplate == null) {
            messageTemplate = new MessageTemplate(templateName, templateText, otherTemplateText);
        } else {
            messageTemplate.setTemplateText(templateText);
            messageTemplate.setOtherText(otherTemplateText);
        }

        messageTemplateService.update(messageTemplate);

        return HttpStatus.OK;
    }

    @ResponseBody
    @PostMapping(value = "/admin/savePicture")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ResponseEntity savePicture(@RequestParam("0") MultipartFile file,
                                      @RequestParam String templateName,
                                      @AuthenticationPrincipal User currentAdmin) {
        try {
            BufferedImage image = ImageIO.read(new BufferedInputStream(file.getInputStream()));
            String fileName = file.getOriginalFilename().replaceFirst("[.][^.]+$", "") + ".png";
            String path = "images/templateName_" + templateName + "/" + fileName;
            File fileTarget = new File((path).replaceAll("/", "\\" + File.separator));
            if (fileTarget.exists()) {
                ImageIO.write(image, "png", fileTarget);
            } else {
                boolean mkdirs = fileTarget.mkdirs();
                if (mkdirs) {
                    ImageIO.write(image, "png", fileTarget);
                }
            }
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(currentAdmin.getId());
    }

}
