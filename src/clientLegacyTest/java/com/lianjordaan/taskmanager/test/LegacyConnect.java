package com.lianjordaan.taskmanager.test;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Connect a private client to its isolated, version-matched test server. */
final class LegacyConnect {
    private LegacyConnect() {
    }

    static void connect(Minecraft client) {
        String address = System.getProperty("taskmanager.test.address", "");
        if (!address.matches("127\\.0\\.0\\.1:[0-9]{4,5}")) {
            throw new AssertionError("A loopback TaskManager test address is required");
        }
        try {
            ServerAddress endpoint = ServerAddress.parseString(address);
            ServerData server = serverData(address);
            Class<?> connectScreen = connectScreen();
            for (Method method : connectScreen.getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class
                    || (types.length != 5 && types.length != 6)
                    || types[0] != Screen.class || types[1] != Minecraft.class
                    || types[2] != ServerAddress.class || types[3] != ServerData.class
                    || types[4] != boolean.class) {
                    continue;
                }
                Object[] args = new Object[types.length];
                args[0] = client.screen != null ? client.screen : new TitleScreen();
                args[1] = client;
                args[2] = endpoint;
                args[3] = server;
                args[4] = false;
                if (types.length == 6) {
                    args[5] = null; // Fresh direct connection, no transfer state.
                }
                method.invoke(null, args);
                System.out.println("TaskManager connecting to " + address);
                return;
            }
            throw new NoSuchMethodException("ConnectScreen.startConnecting");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not connect private TaskManager client", error);
        }
    }

    private static ServerData serverData(String address) throws ReflectiveOperationException {
        for (Constructor<?> constructor : ServerData.class.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length != 3 || types[0] != String.class || types[1] != String.class) {
                continue;
            }
            Object type;
            if (types[2] == boolean.class) {
                type = false;
            } else if (types[2].isEnum() && types[2].getEnumConstants().length == 3) {
                type = types[2].getEnumConstants()[2]; // OTHER, after LAN and REALM.
            } else {
                continue;
            }
            return (ServerData) constructor.newInstance("TaskManager test", address, type);
        }
        throw new NoSuchMethodException("ServerData(name, address, type)");
    }

    private static Class<?> connectScreen() throws ClassNotFoundException {
        String named = "net.minecraft.client.gui.screens.ConnectScreen";
        String runtime = FabricLoader.getInstance().getMappingResolver().mapClassName("named", named);
        try {
            return Class.forName(runtime);
        } catch (ClassNotFoundException ignored) {
            return Class.forName("net.minecraft.class_412");
        }
    }
}
