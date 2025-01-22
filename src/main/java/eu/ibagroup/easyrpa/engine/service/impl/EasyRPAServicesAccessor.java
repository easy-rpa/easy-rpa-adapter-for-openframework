package eu.ibagroup.easyrpa.engine.service.impl;

import eu.easyrpa.openframework.core.model.FileData;
import eu.easyrpa.openframework.core.sevices.RPAServicesAccessor;
import eu.ibagroup.easyrpa.engine.service.ConfigurationService;
import eu.ibagroup.easyrpa.engine.service.NotificationService;
import eu.ibagroup.easyrpa.engine.service.VaultService;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EasyRPAServicesAccessor implements RPAServicesAccessor {

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private VaultService vaultService;

    @Inject
    private NotificationService notificationService;

    @Override
    public String getConfigParam(String key) {
        return configurationService.get(key);
    }

    @Override
    public <T> T getSecret(String alias, Class<T> cls) {
        return vaultService.getSecret(alias, cls);
    }

    @Override
    public void sendMessage(String channelName, String templateName, Map<String, ?> params, List<? extends FileData> files) {
        prepareTempFilesAndRun(files, (tempFiles) -> {
            notificationService.evaluateTemplateAndSend(templateName, params, channelName, tempFiles);
        });
    }

    @Override
    public void sendMessage(String channelName, String subject, String content, List<? extends FileData> files) {
        prepareTempFilesAndRun(files, (tempFiles) -> {
            notificationService.sendNotification(channelName, content, subject, tempFiles);
        });
    }

    @Override
    public byte[] evaluateTemplate(String templateName, Map<String, ?> params) {
        return notificationService.evaluateTemplate(templateName, params);
    }

    private void prepareTempFilesAndRun(List<? extends FileData> files, Consumer<List<File>> action) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("files_to_send");
            List<File> tempFiles = new ArrayList<>();
            for (FileData fileData : files) {
                Path tempFile = Files.createFile(Paths.get(tempDir.toFile().getAbsolutePath(), fileData.getFileName()));
                Files.write(tempFile, fileData.getContent());
                tempFiles.add(tempFile.toFile());
            }
            action.accept(tempFiles);
        } catch (Exception e) {
            throw new RuntimeException("Preparing of files to send has failed.", e);
        } finally {
            try {
                if (tempDir != null) {
                    Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            } catch (Exception e) {
                //do nothing
            }
        }
    }

}
