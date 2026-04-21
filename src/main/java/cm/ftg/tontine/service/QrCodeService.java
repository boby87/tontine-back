package cm.ftg.tontine.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Service de génération de QR Codes pour les invitations de tontine.
 * <p>
 * Le traitement d'image (encodage ZXing + écriture PNG) est délégué à un
 * Virtual Thread via {@code Executors.newVirtualThreadPerTaskExecutor()}
 * afin de ne pas bloquer les I/O du thread appelant.
 */
@Service
public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);

    /** Dimensions du QR Code en pixels */
    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;

    private final ExecutorService notificationExecutor;

    public QrCodeService(ExecutorService notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    /**
     * Génère un QR Code PNG à partir d'une URL d'invitation.
     * Le traitement est exécuté sur un Virtual Thread pour éviter
     * de bloquer les I/O du thread HTTP.
     *
     * @param invitationUrl l'URL à encoder dans le QR Code
     * @return les octets de l'image PNG
     * @throws QrCodeGenerationException si la génération échoue
     */
    public byte[] generateInvitationQrCode(String invitationUrl) {
        try {
            return notificationExecutor.submit(() -> encodeQrCode(invitationUrl)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QrCodeGenerationException("Génération du QR Code interrompue", e);
        } catch (ExecutionException e) {
            throw new QrCodeGenerationException(
                "Erreur lors de la génération du QR Code", e.getCause());
        }
    }

    /**
     * Encode le contenu en QR Code PNG (opération CPU/IO).
     */
    private byte[] encodeQrCode(String content) {
        try {
            var qrCodeWriter = new QRCodeWriter();
            var hints = Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.MARGIN, 2
            );

            var bitMatrix = qrCodeWriter.encode(
                content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

            var outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            log.debug("QR Code généré [{} octets, contenu={}…] — VirtualThread={}",
                outputStream.size(),
                content.length() > 30 ? content.substring(0, 30) : content,
                Thread.currentThread().isVirtual());

            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            throw new QrCodeGenerationException(
                "Impossible d'encoder le QR Code pour : " + content, e);
        }
    }

    /**
     * Exception dédiée à la génération de QR Code.
     */
    public static class QrCodeGenerationException extends RuntimeException {
        public QrCodeGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

