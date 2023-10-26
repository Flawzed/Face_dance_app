/*
 * Copyright 2020 Google LLC. All rights reserved.
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

package com.google.mlkit.vision.demo.java.facedetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;

import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {

  private List<PointF> points;
  private Handler handler;



  private long lastPointGenerationTime = 0;

  private static final float FACE_POSITION_RADIUS = 8.0f;
  private static final float ID_TEXT_SIZE = 30.0f;
  private static final float ID_Y_OFFSET = 40.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;
  private static final int NUM_COLORS = 10;
  protected GraphicOverlay overlay;
  private static final int[][] COLORS =
      new int[][] {
        // {Text color, background color}
        {Color.BLACK, Color.WHITE},
        {Color.WHITE, Color.MAGENTA},
        {Color.BLACK, Color.LTGRAY},
        {Color.WHITE, Color.RED},
        {Color.WHITE, Color.BLUE},
        {Color.WHITE, Color.DKGRAY},
        {Color.BLACK, Color.CYAN},
        {Color.BLACK, Color.YELLOW},
        {Color.WHITE, Color.BLACK},
        {Color.BLACK, Color.GREEN}
      };

  private final Paint facePositionPaint;

  private final Paint RightTopCircle;
  private final Paint LeftTopCircle;
  private final Paint LeftLine;
  private final Paint RightLine;
  private final Paint TopLine;
  private final Paint BottomLine;

  private final Paint[] idPaints;
  private final Paint[] boxPaints;
  private final Paint[] labelPaints;

  private volatile Face face;

  FaceGraphic(GraphicOverlay overlay, Face face) {
    super(overlay);

    this.face = face;
    final int selectedColor = Color.WHITE;

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    LeftTopCircle = new Paint();
    LeftTopCircle.setColor(Color.GRAY);

    RightTopCircle = new Paint();
    RightTopCircle.setColor(Color.GRAY);

    LeftLine = new Paint();
    LeftLine.setColor(Color.GRAY);

    RightLine = new Paint();
    RightLine.setColor(Color.GRAY);

    TopLine = new Paint();
    TopLine.setColor(Color.GRAY);

    BottomLine = new Paint();
    BottomLine.setColor(Color.GRAY);




    int numColors = COLORS.length;
    idPaints = new Paint[numColors];
    boxPaints = new Paint[numColors];
    labelPaints = new Paint[numColors];
    for (int i = 0; i < numColors; i++) {
      idPaints[i] = new Paint();
      idPaints[i].setColor(COLORS[i][0] /* text color */);
      idPaints[i].setTextSize(ID_TEXT_SIZE);

      boxPaints[i] = new Paint();
      boxPaints[i].setColor(COLORS[i][1] /* background color */);
      boxPaints[i].setStyle(Paint.Style.STROKE);
      boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

      labelPaints[i] = new Paint();
      labelPaints[i].setColor(COLORS[i][1] /* background color */);
      labelPaints[i].setStyle(Paint.Style.FILL);
    }
  }
  long pointGenerationInterval = 10000; // 1 секунда
  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    Face face = this.face;
    if (face == null) {
      return;
    }



// Получите текущее время
    long currentTime = System.currentTimeMillis();

// Проверьте, прошло ли достаточно времени с момента последней генерации точки
    if (currentTime - lastPointGenerationTime >= pointGenerationInterval) {
      // Генерируйте случайные координаты для точки
      float randomX = (float) (Math.random() * canvas.getWidth());
      float randomY = (float) (Math.random() * canvas.getHeight());

      // Рисуйте точку
      canvas.drawCircle(randomX, randomY, 30.0f, facePositionPaint);

      // Обновите время последней генерации
      lastPointGenerationTime = currentTime;
    }

    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());

    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);
    canvas.drawCircle(405f, 700f, 15.0f, LeftTopCircle);
    canvas.drawCircle(675f, 700f, 15.0f, RightTopCircle);
    canvas.drawLine(205f, 300f, 205f, 2000f, LeftLine);
    canvas.drawLine(875f, 300f, 875f, 2000f, RightLine);
    canvas.drawLine(305f, 700f, 775f, 700f, TopLine);
    canvas.drawLine(305f, 1700f, 775f, 1700f, BottomLine);


    // Calculate positions.
    float left = x - scale(face.getBoundingBox().width() / 2.0f);
    float top = y - scale(face.getBoundingBox().height() / 2.0f);
    float right = x + scale(face.getBoundingBox().width() / 2.0f);
    float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
    float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
    float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

    // Decide color based on face ID
    int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);




    drawFaceLandmark(canvas, FaceLandmark.NOSE_BASE);
  }


  private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
    FaceLandmark faceLandmark = face.getLandmark(landmarkType);
    if (faceLandmark != null) {
      canvas.drawCircle(
          translateX(faceLandmark.getPosition().x),
          translateY(faceLandmark.getPosition().y),
          FACE_POSITION_RADIUS,
          facePositionPaint);
    }
  }
}
