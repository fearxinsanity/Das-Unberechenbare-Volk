package de.schulprojekt.duv.model.entities;

public class Voter {

    private final long id;
    private double politicalOrientation;
    private double susceptibility;

    public Voter(long id, double politicalOrientation, double susceptibility) {
        this.id = id;
        this.politicalOrientation = politicalOrientation;
        this.susceptibility = susceptibility;
    }

    public long getId() {
        return id;
    }

    public double getPoliticalOrientation() {
        return politicalOrientation;
    }

    public void setPoliticalOrientation(double politicalOrientation) {
        this.politicalOrientation = politicalOrientation;
    }

    public double getSusceptibility() {
        return susceptibility;
    }

    public void setSusceptibility(double susceptibility) {
        this.susceptibility = susceptibility;
    }
}
