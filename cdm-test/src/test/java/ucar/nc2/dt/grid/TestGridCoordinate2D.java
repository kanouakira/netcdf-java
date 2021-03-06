package ucar.nc2.dt.grid;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
@RunWith(JUnit4.class)
public class TestGridCoordinate2D {

  private static void doOne(GridCoordinate2D g2d, double wantLat, double wantLon) {
    int[] result = new int[2];
    if (g2d.findCoordElementForce(wantLat, wantLon, result))
      System.out.printf("Brute (%f %f) == (%d %d) %n", wantLat, wantLon, result[0], result[1]);
    else {
      System.out.printf("Brute (%f %f) FAIL", wantLat, wantLon);
      return;
    }
    if (g2d.findCoordElement(wantLat, wantLon, result))
      System.out.printf("(%f %f) == (%d %d) %n", wantLat, wantLon, result[0], result[1]);
    else
      System.out.printf("(%f %f) FAIL %n", wantLat, wantLon);
    System.out.printf("----------------------------------------%n");
  }

  @Test
  public void testStuff2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/apex_fmrc/Run_20091025_0000.nc";
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("temp");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis lonAxis = gcs.getXHorizAxis();
    assert lonAxis instanceof CoordinateAxis2D;
    CoordinateAxis latAxis = gcs.getYHorizAxis();
    assert latAxis instanceof CoordinateAxis2D;

    GridCoordinate2D g2d = new GridCoordinate2D((CoordinateAxis2D) latAxis, (CoordinateAxis2D) lonAxis);
    doOne(g2d, 40.166959, -73.954234);

    gds.close();
  }

  @Test
  public void testStuff3() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";

    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("Sea_Surface_Height_Relative_to_Geoid_surface");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis lonAxis = gcs.getXHorizAxis();
    assert lonAxis instanceof CoordinateAxis2D;
    CoordinateAxis latAxis = gcs.getYHorizAxis();
    assert latAxis instanceof CoordinateAxis2D;

    GridCoordinate2D g2d = new GridCoordinate2D((CoordinateAxis2D) latAxis, (CoordinateAxis2D) lonAxis);
    doOne(g2d, -15.554099426977835, -0.7742870290336263);

    gds.close();
  }

}
