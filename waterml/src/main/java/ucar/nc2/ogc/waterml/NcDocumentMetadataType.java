package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.DocumentMetadataType;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.time.CalendarDate;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NcDocumentMetadataType {
  // wml2:Collection/wml2:metadata/wml2:DocumentMetadata
  public static DocumentMetadataType initDocumentMetadata(DocumentMetadataType documentMetadata) {
    // @gml:id
    String id = MarshallingUtil.createIdForType(DocumentMetadataType.class);
    documentMetadata.setId(id);

    // wml2:generationDate
    CalendarDate generationDate = MarshallingUtil.fixedGenerationDate;
    if (generationDate == null) {
      generationDate = CalendarDate.present(); // Initialized to "now".
    }
    documentMetadata.setGenerationDate(generationDate.toGregorianCalendar());

    return documentMetadata;
  }

  private NcDocumentMetadataType() {}
}
