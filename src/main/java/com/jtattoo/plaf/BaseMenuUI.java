/*
 * Copyright 2012 - 2020 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jtattoo.plaf;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicMenuUI;

/**
 * @author Michael Hagen
 */
public class BaseMenuUI extends BasicMenuUI {

  protected boolean paintRolloverBorder = true;

  public static ComponentUI createUI(JComponent c) {
    return new BaseMenuUI();
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    c.setOpaque(false);
  }

  @Override
  public void uninstallUI(JComponent c) {
    c.setOpaque(true);
    super.uninstallUI(c);
  }

  @Override
  public void update(Graphics g, JComponent c) {
    paintBackground(g, c, 0, 0, c.getWidth(), c.getHeight());
    paint(g, c);
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();
    Boolean isRolloverEnabled = (Boolean) UIManager.get("MenuBar.rolloverEnabled");
    if (isRolloverEnabled) {
      menuItem.setRolloverEnabled(true);
    }
  }

  @Override
  protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
    if (menuItem.isOpaque()) {
      int w = menuItem.getWidth();
      int h = menuItem.getHeight();
      paintBackground(g, menuItem, 0, 0, w, h);
    }
  }

  protected void paintBackground(Graphics g, JComponent c, int x, int y, int w, int h) {
    JMenuItem mi = (JMenuItem) c;
    Color backColor = mi.getBackground();
    if (backColor == null || backColor instanceof UIResource) {
      backColor = AbstractLookAndFeel.getMenuBackgroundColor();
    }
    ButtonModel model = mi.getModel();
    if (c.getParent() instanceof JMenuBar) {
      if (model.isRollover() || model.isArmed() || (c instanceof JMenu && model.isSelected())) {
        backColor = AbstractLookAndFeel.getMenuSelectionBackgroundColor();
        if (model.isRollover()) {
          backColor = ColorHelper.brighter(backColor, 10);
        }
        g.setColor(backColor);
        g.fillRect(x, y, w, h);
        if (paintRolloverBorder && model.isRollover() && !model.isSelected()) {
          backColor = ColorHelper.darker(backColor, 20);
          g.setColor(backColor);
          g.drawRect(x, y, w - 1, h - 1);
        }
        g.setColor(AbstractLookAndFeel.getMenuSelectionForegroundColor());
      }
    }
    else {
      if (model.isArmed() || model.isRollover() || (c instanceof JMenu && model.isSelected())) {
        g.setColor(AbstractLookAndFeel.getMenuSelectionBackgroundColor());
        g.fillRect(x, y, w, h);
        g.setColor(AbstractLookAndFeel.getMenuSelectionForegroundColor());
      }
      else if (!AbstractLookAndFeel.getTheme().isMenuOpaque()) {
        Graphics2D g2D = (Graphics2D) g;
        Composite savedComposite = g2D.getComposite();
        AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, AbstractLookAndFeel.getTheme().getMenuAlpha());
        g2D.setComposite(alpha);
        g2D.setColor(backColor);
        g2D.fillRect(x, y, w, h);
        g2D.setComposite(savedComposite);
        g2D.setColor(AbstractLookAndFeel.getMenuForegroundColor());
      }
      else {
        g.setColor(backColor);
        g.fillRect(x, y, w, h);
        g.setColor(AbstractLookAndFeel.getMenuForegroundColor());
      }
    }
  }

  @Override
  protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
    ButtonModel model = menuItem.getModel();
    Graphics2D g2D = (Graphics2D) g;
    Object savedRenderingHint = null;
    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
      savedRenderingHint = g2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
      g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AbstractLookAndFeel.getTheme().getTextAntiAliasingHint());
    }
    if (menuItem.getParent() instanceof JMenuBar) {
      if (model.isRollover() || model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
        g.setColor(AbstractLookAndFeel.getMenuSelectionForegroundColor());
      }
    }
    else if (model.isArmed() || model.isRollover()) {
      g.setColor(AbstractLookAndFeel.getMenuSelectionForegroundColor());
    }
    else {
      Color foreColor = menuItem.getForeground();
      if (foreColor instanceof UIResource) {
        foreColor = AbstractLookAndFeel.getMenuForegroundColor();
      }
      g.setColor(foreColor);
    }
    super.paintText(g, menuItem, textRect, text);
    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
      g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedRenderingHint);
    }
  }

  @Override
  protected MouseInputListener createMouseInputListener(JComponent c) {
    return new MyMouseInputHandler();
  }

  // ------------------------------------------------------------------------------
  // inner classes
  // ------------------------------------------------------------------------------

  protected class MyMouseInputHandler extends MouseInputHandler {

    @Override
    public void mouseEntered(MouseEvent evt) {
      super.mouseEntered(evt);

      JMenu menu = (JMenu) evt.getSource();
      if (menu.isTopLevelMenu() && menu.isRolloverEnabled()) {
        menu.getModel().setRollover(true);
        menuItem.repaint();
      }
    }

    @Override
    public void mouseExited(MouseEvent evt) {
      super.mouseExited(evt);

      JMenu menu = (JMenu) evt.getSource();
      ButtonModel model = menu.getModel();
      if (menu.isRolloverEnabled()) {
        model.setRollover(false);
        menuItem.repaint();
      }
    }
  } // end of class MyMouseInputHandler

} // end of class BaseMenuUI
