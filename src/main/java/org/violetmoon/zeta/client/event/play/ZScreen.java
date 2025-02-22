package org.violetmoon.zeta.client.event.play;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.violetmoon.zeta.event.bus.Cancellable;
import org.violetmoon.zeta.event.bus.IZetaPlayEvent;

import java.util.List;

public interface ZScreen extends IZetaPlayEvent {
    Screen getScreen();

    interface Init extends ZScreen {
        List<GuiEventListener> getListenersList();
        void addListener(GuiEventListener listener);
        void removeListener(GuiEventListener listener);

        interface Pre extends Init { }
        interface Post extends Init { }
    }

    interface Render extends ZScreen {
        GuiGraphics getGuiGraphics();
        int getMouseX();
        int getMouseY();

        interface Pre extends Render { }
        interface Post extends Render { }
    }

    interface MouseButtonPressed extends ZScreen, Cancellable {
        int getButton();
        double getMouseX();
        double getMouseY();

        interface Pre extends MouseButtonPressed { }
        interface Post extends MouseButtonPressed { }
    }

    interface MouseScrolled extends ZScreen, Cancellable {
        double getScrollDelta();

        interface Pre extends MouseScrolled { }
        interface Post extends MouseScrolled { }
    }

    interface KeyPressed extends ZScreen, Cancellable {
        int getKeyCode();
        int getScanCode();
        int getModifiers();

        interface Pre extends KeyPressed { }
        interface Post extends KeyPressed { }
    }

    interface CharacterTyped extends ZScreen, Cancellable {
        char getCodePoint();
        int getModifiers();

        interface Pre extends CharacterTyped { }
        interface Post extends CharacterTyped { }
    }

    interface Opening extends ZScreen, Cancellable {
        Screen getCurrentScreen();
        Screen getNewScreen();
        void setNewScreen(Screen newScreen);
    }
}
