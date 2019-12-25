/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.imagerenamer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;

/**
 *
 * @author Easy13
 */
public class ImageHolder extends javax.swing.JPanel {
	private ImageBuffer imageBuffer;
	private BufferedImage imgBF;
	private int angle;
	private double scale;
	private int width;
	private int height;
	private int imageIndex;
	private ArrayList<File> imageList;
	private ImageRenamer imgRenamer;
	private final int scaleType;

	/**
	 * Creates new form ImageHolder
	 */
	public ImageHolder() {
		initComponents();
		imageIndex = 0;
		angle = 0;
		scale = 1;
		scaleType = Image.SCALE_FAST;
	}

	/**
	 * set parent form for manipulate her buttons
	 * 
	 * @param imageRenamer
	 */
	public void setParentForm(ImageRenamer imageRenamer) {
		this.imgRenamer = imageRenamer;
	}

	/**
	 * set ImageBuffer
	 * 
	 * @param imageBuffer
	 */
	public void setBuffer(ImageBuffer imageBuffer) {
		this.imageBuffer = imageBuffer;
	}

	/**
	 * restore ImageHolder from session tmp
	 * 
	 * @param index
	 * @param list
	 * @return
	 */
	public boolean restore(int index, ArrayList<File> list) {
		imageIndex = index;
		imageList = list;
		boolean restored = false;

		if (imageIndex > 0) {
			imgRenamer.enableButtonPrev(true);
		}

		if (imageIndex < imageList.size() && !(imageIndex == imageList.size())) {
			restored = setImage();
		}

		if (imageIndex == imageList.size()) {
			reachedEnd();
			imageIndex--;
			restored = true;
		}
		return restored;
	}

	/**
	 * return number value of pictures, which is now viewed
	 * 
	 * @return
	 */
	public int getImageIndex() {
		return imageIndex;
	}

	/**
	 * sets a list of images to be renamed
	 * 
	 * @param list
	 */
	public void setImagesList(ArrayList<File> list) {
		imageList = list;
		imageIndex = 0;
		setImage();
	}

	/**
	 * display first image called when the set list of images to be renamed
	 * 
	 * @return
	 */
	private boolean setImage() {
		if (imageList.size() > 0) {
			angle = 0;
			scale = 1;

			imgBF = imageBuffer.getCurrentBF();

			int imgWidth = imgBF.getWidth();
			int imgHeight = imgBF.getHeight();

			if (imgWidth >= imgHeight) {
				width = jScrollPane.getViewport().getWidth();
				height = (int) ((((double) width) / imgWidth) * imgHeight);
			} else {
				height = jScrollPane.getViewport().getHeight();
				width = (int) ((((double) height) / imgHeight) * imgWidth);
			}
			Image tmp = imgBF.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = dimg.createGraphics();
			g2d.drawImage(tmp, 0, 0, null);
			g2d.dispose();

			imageLabel.setText("");
			imageLabel.setIcon(new ImageIcon(dimg));

			imgRenamer.enableButtonRL(true);
			imgRenamer.enableButtonRR(true);
			imgRenamer.enableButtonNext(true);
			imgRenamer.enableButtonsZomm(true);
			imgRenamer.enableNewNameField(true);

			return true;
		} else {
			imageLabel.setIcon(null);
			imageLabel.setText("No images to edit");
		}
		return false;
	}

	/**
	 * Displays the next image
	 */
	public void nextImage() throws IOException {
		if (imageIndex < imageList.size() - 1) {
			angle = 0;
			scale = 1;
			imageIndex++;

			imgBF = imageBuffer.getCurrentBF();
			int imgWidth = imgBF.getWidth();
			int imgHeight = imgBF.getHeight();

			if (imgWidth >= imgHeight) {
				width = jScrollPane.getViewport().getWidth();
				height = (int) ((((double) width) / imgWidth) * imgHeight);
			} else {
				height = jScrollPane.getViewport().getHeight();
				width = (int) ((((double) height) / imgHeight) * imgWidth);
			}
			Image tmp = imgBF.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = dimg.createGraphics();
			g2d.drawImage(tmp, 0, 0, null);
			g2d.dispose();

			imageLabel.setText("");
			imageLabel.setIcon(new ImageIcon(dimg));

			imgRenamer.enableButtonPrev(true);
		}
	}

	/**
	 * called when program reached end of image list
	 */
	public void reachedEnd() {
		angle = 0;
		scale = 1;
		imageIndex++;

		imageLabel.setIcon(null);
		imageLabel.setText("Reached end.");

		imgRenamer.enableButtonSave(false);
		imgRenamer.enableButtonPrev(true);
		imgRenamer.enableButtonNext(false);
	}

	/**
	 * Displays the previous image
	 */
	public void prevImage() throws IOException {
		if (imageIndex > 0) {
			angle = 0;
			scale = 1;
			imageIndex--;

			imgBF = imageBuffer.getCurrentBF();
			int imgWidth = imgBF.getWidth();
			int imgHeight = imgBF.getHeight();

			if (imgWidth >= imgHeight) {
				width = jScrollPane.getViewport().getWidth();
				height = (int) ((((double) width) / imgWidth) * imgHeight);
			} else {
				height = jScrollPane.getViewport().getHeight();
				width = (int) ((((double) height) / imgHeight) * imgWidth);
			}
			Image tmp = imgBF.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = dimg.createGraphics();
			g2d.drawImage(tmp, 0, 0, null);
			g2d.dispose();

			imageLabel.setText("");
			imageLabel.setIcon(new ImageIcon(dimg));
			imgRenamer.enableButtonNext(true);
		}
		if (imageIndex == 0) {
			imgRenamer.enableButtonPrev(false);
		}
	}

	/**
	 * rotate right method
	 */
	public void rotateRight() {
		angle += 90;

		Image img;
		double radians = Math.toRadians(angle);
		int newWidth;
		int newHeight;

		if (((Math.abs(angle) / 90) % 2) == 1) {
			newWidth = (int) (height * scale);
			newHeight = (int) (width * scale);
			img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
		} else {
			newWidth = (int) (width * scale);
			newHeight = (int) (height * scale);
			img = imgBF.getScaledInstance(newWidth, newHeight, scaleType);
		}

		BufferedImage rotatedBF = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = rotatedBF.createGraphics();

		int x = (newWidth - img.getWidth(null)) / 2;
		int y = (newHeight - img.getHeight(null)) / 2;

		AffineTransform at = new AffineTransform();
		at.setToRotation(radians, x + (img.getWidth(null) / 2), y + (img.getHeight(null) / 2));
		at.translate(x, y);

		g2d.setTransform(at);
		g2d.drawImage(img, 0, 0, jScrollPane);
		g2d.dispose();

		imageLabel.setIcon(new ImageIcon(rotatedBF));
		jScrollPane.setViewportView(imageLabel);
	}

	/**
	 * rotate left method
	 */
	public void rotateLeft() {
		angle -= 90;

		Image img;
		double radians = Math.toRadians(angle);
		int newWidth;
		int newHeight;

		if (((Math.abs(angle) / 90) % 2) == 1) {
			newWidth = (int) (height * scale);
			newHeight = (int) (width * scale);
			img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
		} else {
			newWidth = (int) (width * scale);
			newHeight = (int) (height * scale);
			img = imgBF.getScaledInstance(newWidth, newHeight, scaleType);
		}

		BufferedImage rotatedBF = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = rotatedBF.createGraphics();

		int x = (newWidth - img.getWidth(null)) / 2;
		int y = (newHeight - img.getHeight(null)) / 2;

		AffineTransform at = new AffineTransform();
		at.setToRotation(radians, x + (img.getWidth(null) / 2), y + (img.getHeight(null) / 2));
		at.translate(x, y);

		g2d.setTransform(at);
		g2d.drawImage(img, 0, 0, jScrollPane);
		g2d.dispose();

		imageLabel.setIcon(new ImageIcon(rotatedBF));
		jScrollPane.setViewportView(imageLabel);
	}

	/**
	 * zoomIn method
	 */
	public void zoomIn() {
		int actualHScrollBarValue = jScrollPane.getHorizontalScrollBar().getValue();
		int actualVScrollBarValue = jScrollPane.getVerticalScrollBar().getValue();

		int maxHScrollBarValueBefore = jScrollPane.getHorizontalScrollBar().getMaximum();
		int maxVScrollBarValueBefore = jScrollPane.getVerticalScrollBar().getMaximum();

		if (imgBF != null) {
			Image img;

			scale *= 1.2;
			double radians = Math.toRadians(angle);
			int newWidth;
			int newHeight;

			if (((Math.abs(angle) / 90) % 2) == 1) {
				newWidth = (int) (height * scale);
				newHeight = (int) (width * scale);
				img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
			} else {
				newWidth = (int) (width * scale);
				newHeight = (int) (height * scale);
				img = imgBF.getScaledInstance(newWidth, newHeight, scaleType);
			}

			BufferedImage rotatedBF = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = rotatedBF.createGraphics();

			int x = (newWidth - img.getWidth(null)) / 2;
			int y = (newHeight - img.getHeight(null)) / 2;

			AffineTransform at = new AffineTransform();
			at.setToRotation(radians, x + (img.getWidth(null) / 2), y + (img.getHeight(null) / 2));
			at.translate(x, y);

			g2d.setTransform(at);
			g2d.drawImage(img, 0, 0, jScrollPane);
			g2d.dispose();

			imageLabel.setIcon(new ImageIcon(rotatedBF));
			jScrollPane.setViewportView(imageLabel);
		}

		/* set ScrollBar position after zoom */
		int maxHScrollBarValueAfter = jScrollPane.getHorizontalScrollBar().getMaximum();
		int maxVScrollBarValueAfter = jScrollPane.getVerticalScrollBar().getMaximum();

		boolean isHScrollBarActive = jScrollPane.getHorizontalScrollBar().isVisible();
		boolean isVScrollBarActive = jScrollPane.getVerticalScrollBar().isVisible();

		/* set Horizontal position */
		if (!isHScrollBarActive) {
			jScrollPane.getHorizontalScrollBar()
					.setValue((maxHScrollBarValueAfter - jScrollPane.getViewport().getViewRect().width) / 2);
		} else {
			jScrollPane.getHorizontalScrollBar().setValue(
					(int) (((double) actualHScrollBarValue / maxHScrollBarValueBefore) * maxHScrollBarValueAfter));
		}
		/* set vertical position */
		if (!isVScrollBarActive) {
			jScrollPane.getVerticalScrollBar()
					.setValue((maxVScrollBarValueAfter - jScrollPane.getViewport().getViewRect().height) / 2);
		} else {
			jScrollPane.getVerticalScrollBar().setValue(
					(int) (((double) actualVScrollBarValue / maxVScrollBarValueBefore) * maxVScrollBarValueAfter));
		}
	}

	/**
	 * zoomOut method
	 */
	public void zoomOut() {
		int actualHScrollBarValue = jScrollPane.getHorizontalScrollBar().getValue();
		int actualVScrollBarValue = jScrollPane.getVerticalScrollBar().getValue();

		int maxHScrollBarValueBefore = jScrollPane.getHorizontalScrollBar().getMaximum();
		int maxVScrollBarValueBefore = jScrollPane.getVerticalScrollBar().getMaximum();

		if (imgBF != null) {
			Image img;

			scale /= 1.2;
			double radians = Math.toRadians(angle);
			int newWidth;
			int newHeight;

			if (((Math.abs(angle) / 90) % 2) == 1) {
				newWidth = (int) (height * scale);
				newHeight = (int) (width * scale);
				img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
			} else {
				newWidth = (int) (width * scale);
				newHeight = (int) (height * scale);
				img = imgBF.getScaledInstance(newWidth, newHeight, scaleType);
			}

			BufferedImage rotatedBF = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = rotatedBF.createGraphics();

			int x = (newWidth - img.getWidth(null)) / 2;
			int y = (newHeight - img.getHeight(null)) / 2;

			AffineTransform at = new AffineTransform();
			at.setToRotation(radians, x + (img.getWidth(null) / 2), y + (img.getHeight(null) / 2));
			at.translate(x, y);

			g2d.setTransform(at);
			g2d.drawImage(img, 0, 0, jScrollPane);
			g2d.dispose();

			imageLabel.setIcon(new ImageIcon(rotatedBF));
			jScrollPane.setViewportView(imageLabel);
		}

		/* set ScrollBar position after zoom */
		int maxHScrollBarValueAfter = jScrollPane.getHorizontalScrollBar().getMaximum();
		int maxVScrollBarValueAfter = jScrollPane.getVerticalScrollBar().getMaximum();

		boolean isHScrollBarActive = jScrollPane.getHorizontalScrollBar().isVisible();
		boolean isVScrollBarActive = jScrollPane.getVerticalScrollBar().isVisible();

		/* set Horizontal position */
		if (!isHScrollBarActive) {
			jScrollPane.getHorizontalScrollBar()
					.setValue((maxHScrollBarValueAfter - jScrollPane.getViewport().getViewRect().width) / 2);
		} else {
			jScrollPane.getHorizontalScrollBar().setValue(
					(int) (((double) actualHScrollBarValue / maxHScrollBarValueBefore) * maxHScrollBarValueAfter));
		}
		/* set vertical position */
		if (!isVScrollBarActive) {
			jScrollPane.getVerticalScrollBar()
					.setValue((maxVScrollBarValueAfter - jScrollPane.getViewport().getViewRect().height) / 2);
		} else {
			jScrollPane.getVerticalScrollBar().setValue(
					(int) (((double) actualVScrollBarValue / maxVScrollBarValueBefore) * maxVScrollBarValueAfter));
		}
	}

	/**
	 * move scrollbar position left
	 */
	public void moveImgLeft() {
		int maxHScrollBarValue = jScrollPane.getHorizontalScrollBar().getMaximum();
		int actualHScrollBarValue = jScrollPane.getHorizontalScrollBar().getValue();
		boolean isHScrollBarActive = jScrollPane.getHorizontalScrollBar().isVisible();

		if (isHScrollBarActive) {
			jScrollPane.getHorizontalScrollBar().setValue((int) (actualHScrollBarValue - maxHScrollBarValue * 0.1));
		}
	}

	/**
	 * move scrollbar position right
	 */
	public void moveImgRight() {
		int maxHScrollBarValue = jScrollPane.getHorizontalScrollBar().getMaximum();
		int actualHScrollBarValue = jScrollPane.getHorizontalScrollBar().getValue();
		boolean isHScrollBarActive = jScrollPane.getHorizontalScrollBar().isVisible();

		if (isHScrollBarActive) {
			jScrollPane.getHorizontalScrollBar().setValue((int) (actualHScrollBarValue + maxHScrollBarValue * 0.1));
		}
	}

	/**
	 * move scrollbar position up
	 */
	public void moveImgUp() {
		int maxVScrollBarValue = jScrollPane.getVerticalScrollBar().getMaximum();
		int actualVScrollBarValue = jScrollPane.getVerticalScrollBar().getValue();
		boolean isVScrollBarActive = jScrollPane.getVerticalScrollBar().isVisible();

		if (isVScrollBarActive) {
			jScrollPane.getVerticalScrollBar().setValue((int) (actualVScrollBarValue - maxVScrollBarValue * 0.1));
		}
	}

	/**
	 * move scrollbar position down
	 */
	public void moveImgDown() {
		int maxVScrollBarValue = jScrollPane.getVerticalScrollBar().getMaximum();
		int actualVScrollBarValue = jScrollPane.getVerticalScrollBar().getValue();
		boolean isVScrollBarActive = jScrollPane.getVerticalScrollBar().isVisible();

		if (isVScrollBarActive) {
			jScrollPane.getVerticalScrollBar().setValue((int) (actualVScrollBarValue + maxVScrollBarValue * 0.1));
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jScrollPane = new javax.swing.JScrollPane();
		imageLabel = new javax.swing.JLabel();

		addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent evt) {
				formComponentResized(evt);
			}
		});

		jScrollPane.setBorder(null);

		imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		imageLabel.setText("No images to edit");
		jScrollPane.setViewportView(imageLabel);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE));
	}// </editor-fold>//GEN-END:initComponents

	/**
	 * called when the form was changed
	 * 
	 * @param evt
	 */
	private void formComponentResized(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentResized
		if (imgBF != null) {
			Image img;

			double radians = Math.toRadians(angle);
			int newWidth;
			int newHeight;

			int imgWidth = imgBF.getWidth();
			int imgHeight = imgBF.getHeight();

			if (imgWidth >= imgHeight) {
				width = jScrollPane.getViewport().getWidth();
				height = (int) ((((double) width) / imgWidth) * imgHeight);
			} else {
				height = jScrollPane.getViewport().getHeight();
				width = (int) ((((double) height) / imgHeight) * imgWidth);
			}

			if (((Math.abs(angle) / 90) % 2) == 1) {
				if (width >= height) {
					newHeight = (int) (jScrollPane.getViewport().getHeight() * scale);
					newWidth = (int) (((double) newHeight / width) * height * scale);
					img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
				} else {
					newWidth = (int) (imageLabel.getWidth() * scale);
					newHeight = (int) (width * ((double) newWidth / height) * scale);
					img = imgBF.getScaledInstance(newHeight, newWidth, scaleType);
				}
			} else {
				newWidth = (int) (width * scale);
				newHeight = (int) (height * scale);
				img = imgBF.getScaledInstance(newWidth, newHeight, scaleType);
			}

			BufferedImage rotatedBF = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = rotatedBF.createGraphics();

			int x = (newWidth - img.getWidth(null)) / 2;
			int y = (newHeight - img.getHeight(null)) / 2;

			AffineTransform at = new AffineTransform();
			at.setToRotation(radians, x + (img.getWidth(null) / 2), y + (img.getHeight(null) / 2));
			at.translate(x, y);

			g2d.setTransform(at);
			g2d.drawImage(img, 0, 0, jScrollPane);
			g2d.dispose();

			imageLabel.setIcon(new ImageIcon(rotatedBF));
			jScrollPane.setViewportView(imageLabel);

			javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
			this.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE));
			layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE));
		}
	}// GEN-LAST:event_formComponentResized

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel imageLabel;
	private javax.swing.JScrollPane jScrollPane;
	// End of variables declaration//GEN-END:variables

	/**
	 * Inner class representing a stack of imageLabel and methods of work with him.
	 * Used for faster preparation for displaying next picture
	 */
//    private class ImageStack{
//        
//        private LinkedList<JLabel> labelList;
//
//        /**
//         * Creates new ImageStack
//         */
//        public ImageStack() {
//            labelList = new LinkedList<JLabel>();
//            
//            JLabel labelPrev = new JLabel();
//            labelPrev.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//            labelPrev.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
//            
//            JLabel labelActual = new JLabel();
//            labelActual.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//            labelActual.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
//            
//            JLabel labelNext = new JLabel();
//            labelNext.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//            labelNext.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
//            
//            labelList.add(labelPrev);
//            labelList.add(labelActual);
//            labelList.add(labelNext);
//        }
//        
//        public JLabel getPrevLabel(){
//            return labelList.get(0);
//        }
//        
//        public JLabel getActualLabel(){
//            return labelList.get(1);
//        }
//        
//        public JLabel getNextLabel(){
//            return labelList.get(2);
//        }   
//        
//        public void stepNext(){
//            labelList.removeFirst();
//            JLabel labelNext = new JLabel();
//            labelNext.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//            labelNext.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
//            labelList.addLast(labelNext);
//        }
//        
//        public void stepPrev(){
//            labelList.removeLast();
//            JLabel labelPrev = new JLabel();
//            labelPrev.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//            labelPrev.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
//            labelList.addFirst(labelPrev);
//        }
//    }
}
