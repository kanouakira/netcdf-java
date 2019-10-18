package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.transform.VertTransformBuilderIF;

/** A helper class for NetcdfDataset to build and manage coordinates */
@Immutable
public class CoordinatesHelper {
  private final List<CoordinateAxis> coordAxes;
  private final List<CoordinateSystem> coordSys;
  private final List<CoordinateTransform> coordTransforms;

  public List<CoordinateAxis> getCoordAxes() {
    return coordAxes;
  }

  public List<CoordinateSystem> getCoordSystems() {
    return coordSys;
  }

  public Optional<CoordinateSystem> findCoordSystem(String name) {
    return coordSys.stream().filter(cs -> cs.getName().equals(name)).findFirst();
  }

  public List<CoordinateTransform> getCoordTransforms() {
    return coordTransforms;
  }

  private CoordinatesHelper(Builder builder, NetcdfDataset ncd) {
    this.coordAxes = new ArrayList<>();
    for (Variable v : ncd.getVariables()) {
      if (v instanceof CoordinateAxis) {
        this.coordAxes.add((CoordinateAxis) v);
      }
    }
    this.coordTransforms = builder.coordTransforms.stream().map(ct -> ct.build(ncd))
        .filter(Objects::nonNull).collect(Collectors.toList());
    this.coordSys = builder.coordSys.stream().map(s -> s.build(ncd, this.coordAxes,  this.coordTransforms)).collect(Collectors.toList());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    public List<CoordinateAxis.Builder> coordAxes = new ArrayList<>();
    public List<CoordinateSystem.Builder> coordSys = new ArrayList<>();
    public List<CoordinateTransform.Builder> coordTransforms = new ArrayList<>();
    private boolean built;

    public Builder addCoordinateAxis(CoordinateAxis.Builder axis) {
      coordAxes.add(axis);
      return this;
    }

    public Builder addCoordinateAxes(Collection<CoordinateAxis.Builder> axes) {
      axes.forEach(a -> addCoordinateAxis(a));
      return this;
    }

    public Optional<CoordinateAxis.Builder> findCoordinateAxis(String axisName) {
      return coordAxes.stream().filter(axis -> axis.shortName.equals(axisName)).findFirst();
    }

    // LOOK dedup
    public Builder addCoordinateSystem(CoordinateSystem.Builder cs) {
      coordSys.add(cs);
      return this;
    }

    public Optional<CoordinateSystem.Builder> findCoordinateSystem(String coordAxesNames) {
      return coordSys.stream().filter(cs -> cs.coordAxesNames.equals(coordAxesNames)).findFirst();
    }

    public Builder addCoordinateSystems(Collection<CoordinateSystem.Builder> systems) {
      coordSys.addAll(systems);
      return this;
    }

    public Builder addCoordinateTransform(CoordinateTransform.Builder ct) {
      if (!findCoordinateTransform(ct.name).isPresent()) {
        coordTransforms.add(ct);
      }
      return this;
    }

    public Builder addCoordinateTransforms(Collection<CoordinateTransform.Builder> transforms) {
      transforms.forEach(ct -> addCoordinateTransform(ct));
      return this;
    }

    public Optional<CoordinateTransform.Builder> findCoordinateTransform(String ctName) {
      return coordTransforms.stream().filter(ct -> ct.name.equals(ctName)).findFirst();
    }

    public List<CoordinateAxis.Builder> getAxesForSystem(CoordinateSystem.Builder cs) {
      Preconditions.checkNotNull(cs);
      List<CoordinateAxis.Builder> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(cs.coordAxesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder> vbOpt = findCoordinateAxis(vname);
        if (vbOpt.isPresent()) {
          axes.add(vbOpt.get());
        } else {
          throw new IllegalArgumentException("Cant find axis " + vname);
        }
      }
      return axes;
    }

    public String makeCanonicalName(String axesNames) {
      Preconditions.checkNotNull(axesNames);
      List<CoordinateAxis.Builder> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(axesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder> vbOpt = findCoordinateAxis(vname);
        if (vbOpt.isPresent()) {
          axes.add(vbOpt.get());
        } else {
          throw new IllegalArgumentException("Cant find axis " + vname);
        }
      }
      return makeCanonicalName(axes);
    }

    public String makeCanonicalName(List<CoordinateAxis.Builder> axes) {
      return axes.stream().sorted(new AxisComparator()).map(a -> a.shortName).collect(Collectors.joining(" "));
    }

    public CoordinatesHelper build(NetcdfDataset ncd) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinatesHelper(this, ncd);
    }

    // Check if this Coordinate System is complete for v, ie its dimensions match v.
    public boolean isComplete(CoordinateSystem.Builder<?> cs, VariableDS.Builder<?> vb) {
      Set<Dimension> varDomain = new HashSet<>(vb.dimensions);
      Set<Dimension> csDomain = new HashSet<>();
      getAxesForSystem(cs).forEach(axis -> csDomain.addAll(axis.dimensions));
      return varDomain.equals(csDomain);
    }

    public boolean containsAxes(CoordinateSystem.Builder cs, List<CoordinateAxis.Builder> dataAxes) {
      List<CoordinateAxis.Builder> csAxes = getAxesForSystem(cs);
      return csAxes.containsAll(dataAxes);
    }

    public boolean containsAxisTypes(CoordinateSystem.Builder cs, List<AxisType> axisTypes) {
      List<CoordinateAxis.Builder> csAxes = getAxesForSystem(cs);
      for (AxisType axisType : axisTypes) {
        if (!containsAxisTypes(csAxes, axisType))
          return false;
      }
      return true;
    }

    private boolean containsAxisTypes(List<CoordinateAxis.Builder> axes, AxisType want) {
      for (CoordinateAxis.Builder axis : axes) {
        if (axis.axisType == want)
          return true;
      }
      return false;
    }
  }

  private static class AxisComparator implements java.util.Comparator<CoordinateAxis.Builder> {
    public int compare(CoordinateAxis.Builder c1, CoordinateAxis.Builder c2) {

      AxisType t1 = c1.axisType;
      AxisType t2 = c2.axisType;

      if ((t1 == null) && (t2 == null))
        return c1.shortName.compareTo(c2.shortName);
      if (t1 == null)
        return -1;
      if (t2 == null)
        return 1;

      return t1.axisOrder() - t2.axisOrder();
    }
  }
}
