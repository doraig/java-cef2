// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;

/**
 * Client that owns a browser and renderer.
 */
public class CefClient implements CefHandler {
  private CefClientDelegate delegate_;
  private CefRenderer renderer_;
  private GLCanvas canvas_;
  private long window_handle_ = 0;
  private CefBrowser browser_ = null;
  private int browser_id_ = -1;
  private Rectangle browser_rect_ = new Rectangle(0, 0, 0, 0);

  public CefClient(CefClientDelegate delegate, boolean transparent) {
    assert(delegate != null);
    delegate_ = delegate;
    renderer_ = new CefRenderer(transparent);
    createGLCanvas();
  }
  
  @Override
  protected void finalize() throws Throwable {
    if (browser_ != null)
      destroyBrowser();
    super.finalize();
  }

  public void createBrowser(String url) {
    assert(browser_ == null);
    browser_ = CefContext.createBrowser(this, getWindowHandle(), url, renderer_.isTransparent());
    browser_id_ = browser_.getIdentifier();
  }

  public void destroyBrowser() {
    assert(browser_ != null);
    browser_.close();
    browser_ = null;
    browser_id_ = -1;
  }

  public CefBrowser getBrowser() {
    return browser_;
  }

  public GLCanvas getCanvas() {
    return canvas_;
  }

  public long getWindowHandle() {
    if (window_handle_ == 0) {
      NativeSurface surface = canvas_.getNativeSurface();
      surface.lockSurface();
      window_handle_ = CefContext.getWindowHandle(surface.getSurfaceHandle());
      surface.unlockSurface();
      assert (window_handle_ != 0);
    }
    return window_handle_;
  }

  private void createGLCanvas() {
    GLProfile glprofile = GLProfile.getDefault();
    GLCapabilities glcapabilities = new GLCapabilities(glprofile);
    canvas_ = new GLCanvas(glcapabilities);

    canvas_.addGLEventListener(new GLEventListener() {
      @Override
      public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
        browser_rect_.setBounds(x, y, width, height);
        if (browser_ != null)
          browser_.wasResized();
      }

      @Override
      public void init(GLAutoDrawable glautodrawable) {
        renderer_.initialize(glautodrawable.getGL().getGL2());
      }

      @Override
      public void dispose(GLAutoDrawable glautodrawable) {
        renderer_.cleanup(glautodrawable.getGL().getGL2());
      }

      @Override
      public void display(GLAutoDrawable glautodrawable) {
        renderer_.render(glautodrawable.getGL().getGL2());
      }
    });

    canvas_.addMouseListener(new MouseListener() {
      @Override
      public void mousePressed(MouseEvent e) {
        onMouseEvent(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        onMouseEvent(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        onMouseEvent(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        onMouseEvent(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        onMouseEvent(e);
      }
    });

    canvas_.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseMoved(MouseEvent e) {
        onMouseEvent(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        onMouseEvent(e);
      }
    });

    canvas_.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        onMouseWheelEvent(e);
      }
    });

    canvas_.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {
        onKeyEvent(e);
      }

      @Override
      public void keyPressed(KeyEvent e) {
        onKeyEvent(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        onKeyEvent(e);
      }
    });
  }

  private void onMouseEvent(MouseEvent e) {
    if (browser_ != null)
      browser_.sendMouseEvent(e);
  }
  
  private void onMouseWheelEvent(MouseWheelEvent e) {
    if (browser_ != null)
      browser_.sendMouseWheelEvent(e);
  }

  private void onKeyEvent(KeyEvent e) {
    if (browser_ != null)
      browser_.sendKeyEvent(e);
  }

  @Override
  public void onAfterCreated(CefBrowser browser) {
  }

  @Override
  public void onAddressChange(CefBrowser browser, String url) {
    if (browser_.getIdentifier() == browser_id_)
      delegate_.onAddressChange(this, url);
  }

  @Override
  public void onTitleChange(CefBrowser browser, String title) {
    if (browser_.getIdentifier() == browser_id_)
      delegate_.onTitleChange(this, title);
  }

  @Override
  public Rectangle getViewRect(CefBrowser browser) {
    return browser_rect_;
  }

  @Override
  public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
    Point screenPoint = canvas_.getLocationOnScreen();
    screenPoint.translate(viewPoint.x, viewPoint.y);
    return screenPoint;
  }

  @Override
  public void onPopupShow(CefBrowser browser,
                          boolean show) {
    if (!show) {
      Rectangle old_rect = renderer_.getPopupRect();
      renderer_.clearPopupRects();
      browser_.invalidate(old_rect);
    }
  }

  @Override
  public void onPopupSize(CefBrowser browser,
                          Rectangle size) {
    renderer_.onPopupSize(size);
  }

  @Override
  public void onPaint(CefBrowser browser,
                      boolean popup,
                      Rectangle[] dirtyRects,
                      ByteBuffer buffer,
                      int width,
                      int height) {
    canvas_.getContext().makeCurrent();
    renderer_.onPaint(canvas_.getGL().getGL2(), popup, dirtyRects, buffer, width, height);
    canvas_.getContext().release();
    canvas_.display();
  }
  
  @Override
  public void onCursorChange(CefBrowser browser,
                             int cursorType) {
    delegate_.onCursorChange(this, new Cursor(cursorType));
  }
}