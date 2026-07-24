package com.dupeclient.client.module.packet.sniffer;

import java.lang.reflect.Type;
import org.jetbrains.annotations.Nullable;

public final class PacketFieldModel {
    public final String name;
    public final String typeName;
    public final String value;
    public final boolean editable;
    @Nullable
    public final Class<?> valueType;
    @Nullable
    public final Type genericType;

    public PacketFieldModel(String name, String typeName, String value, boolean editable, @Nullable Class<?> valueType) {
        this(name, typeName, value, editable, valueType, null);
    }

    public PacketFieldModel(String name, String typeName, String value, boolean editable, @Nullable Class<?> valueType, @Nullable Type genericType) {
        this.name = name;
        this.typeName = typeName;
        this.value = value;
        this.editable = editable;
        this.valueType = valueType;
        this.genericType = genericType;
    }

    public PacketFieldModel withValue(String newValue) {
        return new PacketFieldModel(name, typeName, newValue, editable, valueType, genericType);
    }
}
