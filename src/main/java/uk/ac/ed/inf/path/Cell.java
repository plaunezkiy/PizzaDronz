package uk.ac.ed.inf.path;

import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.Objects;

public class Cell {
    LngLat coordinates;
    double f;
    double enterAngle;

    public Cell(LngLat coordinates, double enterAngle) {
        this.coordinates = coordinates;
        this.f = 0;
        this.enterAngle = enterAngle;
    }

    public double getEnterAngle() {
        return enterAngle;
    }

    public LngLat getCoordinates() {
        return coordinates;
    }

    public void setF(double value) {
        this.f = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates.lat(), coordinates.lng());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Cell other = (Cell) obj;
        return this.hashCode() == other.hashCode();
    }
}
