package ru.armagidon.poseplugin.api.poses;

public enum EnumPose
{

    STANDING("stand"),
    SITTING("sit"),
    LYING("lay"),
    SWIMMING("swim");

    private final String name;

    EnumPose(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}