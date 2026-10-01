package tn.iset.gabes.helpdesk.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generation des etiquettes QR collees sur les materiels.
 * <p>
 * Le niveau de correction d'erreur est volontairement eleve : ces
 * etiquettes sont apposees sur des machines en cuisine ou derriere un
 * comptoir, ou elles se salissent et s'usent. Un niveau HAUT permet au
 * code de rester lisible meme partiellement abime.
 */
@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 300;

    /**
     * Produit une image PNG contenant le QR code du contenu fourni.
     *
     * @param content texte a encoder, ici l'URL de declaration de panne
     * @param size    cote de l'image en pixels
     * @return les octets de l'image PNG
     */
    public byte[] generatePng(String content, int size) {
        int finalSize = size > 0 ? size : DEFAULT_SIZE;
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 2
            );

            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, finalSize, finalSize, hints);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();

        } catch (WriterException | IOException ex) {
            throw new BusinessException(
                    "La generation du QR code a echoue : " + ex.getMessage());
        }
    }

    public byte[] generatePng(String content) {
        return generatePng(content, DEFAULT_SIZE);
    }
}
