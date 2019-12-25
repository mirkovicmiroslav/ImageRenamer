/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.imagerenamer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author Easy13
 */
public class PrevImageHolder extends javax.swing.JPanel {   
    private ImageBuffer imageBuffer;
    private ArrayList<File> imageList;
    private HashMap<File, ArrayList<String>> imageRenameMap;
    private BufferedImage imgBF;
    private BufferedImage imgBFNext;
    private BufferedImage imgBFPrev;
    private int width;
    private int height;
    private int imageIndex;
    private final int scaleType;
    private int maxImgSize = 2000*3000;
    private int maxImgSide = 3000;

    /**
     * Creates new form PrevImageHolder
     */
    public PrevImageHolder() {
        initComponents();
        imageIndex = 0;
        scaleType = Image.SCALE_FAST;
    }
    
    /**
     * set ImageBuffer
     * @param imageBuffer 
     */
    public void setBuffer(ImageBuffer imageBuffer){
        this.imageBuffer = imageBuffer;
    }
    
    /**
     * restore previewImageHolder from session tmp
     * @param index
     * @param list
     * @param map
     * @return 
     */
    public boolean restore(int index, ArrayList<File> list, HashMap<File, ArrayList<String>> map){
        imageIndex = index;
        imageList = list;
        imageRenameMap = map;
        
        jLabelImgPrev.setIcon(null);
        jLabelImgPrev.setText("No data");
        jLabelImgCurrent.setIcon(null);
        jLabelImgCurrent.setText("No data");
        jLabelImgNext.setIcon(null);
        jLabelImgNext.setText("No data");
        
        setListModel();
        
        /*set previous image preview*/
        if(imageIndex > 0){
            try {                
                BufferedImage newImg = ImageIO.read(imageList.get(imageIndex - 1));
                imageBuffer.setPreviousBF(newImg);
                int imgWidth = newImg.getWidth();
                int imgHeight = newImg.getHeight();

                if(imgWidth >= imgHeight){
                    width = jLabelImgPrev.getWidth();
                    height = (int)((((double)width) / imgWidth) * imgHeight);
                }
                else{
                    height = jLabelImgPrev.getHeight();
                    width = (int)((((double)height) / imgHeight) * imgWidth);              
                }

                imgBFPrev = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                Graphics2D g = imgBFPrev.createGraphics();
                g.drawImage(newImg, 0, 0, width, height, null);
                g.dispose();

                jLabelImgPrev.setText("");
                jLabelImgPrev.setIcon(new ImageIcon(imgBFPrev));              
            } catch (IOException ex) {
                Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        
        /* Prepare present preview image */
        if(imageIndex < imageList.size()){
            try{
                BufferedImage newImg = ImageIO.read(imageList.get(imageIndex));
                imageBuffer.setCurrentBF(newImg);
                int imgWidth = newImg.getWidth();
                int imgHeight = newImg.getHeight();

                if(imgWidth >= imgHeight){
                    width = jLabelImgCurrent.getWidth();
                    height = (int)((((double)width) / imgWidth) * imgHeight);
                }
                else{
                    height = jLabelImgCurrent.getHeight();
                    width = (int)((((double)height) / imgHeight) * imgWidth);              
                }
                
                imgBF = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                Graphics2D g = imgBF.createGraphics();
                g.drawImage(newImg, 0, 0, width, height, null);
                g.dispose();

                jLabelImgCurrent.setText("");
                jLabelImgCurrent.setIcon(new ImageIcon(imgBF));
            } catch (IOException ex) {
                Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
            
        /* Prepare next preview image */
        try {
            if(imageIndex + 1 < imageList.size()){
                BufferedImage newImg = ImageIO.read(imageList.get(imageIndex + 1));
                imageBuffer.setNextBF(newImg);
                int imgWidth = newImg.getWidth();
                int imgHeight = newImg.getHeight();

                if(imgWidth >= imgHeight){
                    width = jLabelImgNext.getWidth();
                    height = (int)((((double)width) / imgWidth) * imgHeight);
                }
                else{
                    height = jLabelImgNext.getHeight();
                    width = (int)((((double)height) / imgHeight) * imgWidth);              
                }

                imgBFNext = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                Graphics2D g = imgBFNext.createGraphics();
                g.drawImage(newImg, 0, 0, width, height, null);
                g.dispose();

                jLabelImgNext.setText("");
                jLabelImgNext.setIcon(new ImageIcon(imgBFNext));
            }else{
                imgBFNext = null;
            }
        } catch (IOException ex) {
            Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
    
    /**
     * set ImageList and HashMap with key = file and value = his new names
     * @param list
     * @param map 
     */
    public void setImageList(ArrayList<File> list, HashMap<File, ArrayList<String>> map){        
        imageList = list;
        imageRenameMap = map;
        imageIndex = 0;
        
        jLabelImgPrev.setIcon(null);
        jLabelImgPrev.setText("No data");
        jLabelImgCurrent.setIcon(null);
        jLabelImgCurrent.setText("No data");
        jLabelImgNext.setIcon(null);
        jLabelImgNext.setText("No data");
        
        setListModel();
        
        if(imageList.size() > 0){
            /* Prepare present preview image */
            try{
                BufferedImage newImg = ImageIO.read(imageList.get(imageIndex));
//                imageBuffer.setCurrentBF(newImg);
//                int imgWidth = newImg.getWidth();
//                int imgHeight = newImg.getHeight();
                
                int imgWidth = newImg.getWidth();
                int imgHeight = newImg.getHeight();
                
                if(imgWidth * imgHeight <= maxImgSize){
                    imageBuffer.setCurrentBF(newImg);
                }else{                    
                    if(imgWidth >= imgHeight){
                        width = maxImgSide;
                        height = (int)((((double)width) / imgWidth) * imgHeight);
                    }
                    else{
                        height = maxImgSide;
                        width = (int)((((double)height) / imgHeight) * imgWidth);              
                    }

                    BufferedImage imgBFnew = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                    Graphics2D g = imgBFnew.createGraphics();
                    g.drawImage(newImg, 0, 0, width, height, null);
                    g.dispose();
                    imageBuffer.setCurrentBF(imgBFnew);
                }

                if(imgWidth >= imgHeight){
                    width = jLabelImgCurrent.getWidth();
                    height = (int)((((double)width) / imgWidth) * imgHeight);
                }
                else{
                    height = jLabelImgCurrent.getHeight();
                    width = (int)((((double)height) / imgHeight) * imgWidth);              
                }
                
                imgBF = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                Graphics2D g = imgBF.createGraphics();
                g.drawImage(newImg, 0, 0, width, height, null);
                g.dispose();

                jLabelImgCurrent.setText("");
                jLabelImgCurrent.setIcon(new ImageIcon(imgBF));
            } catch (IOException ex) {
                Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /* Prepare next preview image */
            try {
                if(imageIndex + 1 < imageList.size()){
                    BufferedImage newImg = ImageIO.read(imageList.get(imageIndex + 1));
//                    imageBuffer.setNextBF(newImg);
//                    int imgWidth = newImg.getWidth();
//                    int imgHeight = newImg.getHeight();

                    int imgWidth = newImg.getWidth();
                    int imgHeight = newImg.getHeight();

                    if(imgWidth * imgHeight <= maxImgSize){
                        imageBuffer.setNextBF(newImg);
                    }else{
                        if(imgWidth >= imgHeight){
                            width = maxImgSide;
                            height = (int)((((double)width) / imgWidth) * imgHeight);
                        }
                        else{
                            height = maxImgSide;
                            width = (int)((((double)height) / imgHeight) * imgWidth);              
                        }

                        BufferedImage imgBFnew = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                        Graphics2D g = imgBFnew.createGraphics();
                        g.drawImage(newImg, 0, 0, width, height, null);
                        g.dispose();
                        imageBuffer.setNextBF(imgBFnew);
                    }
                    
                    if(imgWidth >= imgHeight){
                        width = jLabelImgNext.getWidth();
                        height = (int)((((double)width) / imgWidth) * imgHeight);
                    }
                    else{
                        height = jLabelImgNext.getHeight();
                        width = (int)((((double)height) / imgHeight) * imgWidth);              
                    }
                    
                    imgBFNext = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                    Graphics2D g = imgBFNext.createGraphics();
                    g.drawImage(newImg, 0, 0, width, height, null);
                    g.dispose();

                    jLabelImgNext.setText("");
                    jLabelImgNext.setIcon(new ImageIcon(imgBFNext));
                }else{
                    imgBFNext = null;
                }
            } catch (IOException ex) {
                Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * display next preview image
     */
    public void next() throws IOException{
        if(imageIndex < imageList.size()){
            imageIndex++;
            imgBFPrev = imgBF;
            imgBF = imgBFNext;
            
            imageBuffer.setPreviousBF(imageBuffer.getCurrentBF());
            imageBuffer.setCurrentBF(imageBuffer.getNextBF());
            
            /*set previous image preview*/
            jLabelImgPrev.setText("");
            jLabelImgPrev.setIcon(new ImageIcon(imgBFPrev));

            /*set present preview image*/
            if(imageIndex == imageList.size()){
                imgBF = null;
                jLabelImgCurrent.setIcon(null);
                jLabelImgCurrent.setText("No data");
            }else{
                jLabelImgCurrent.setText("");
                jLabelImgCurrent.setIcon(new ImageIcon(imgBF));
            }            

            /*set next preview image*/
            if(imageIndex >= imageList.size() - 1){
                imgBFNext = null;
                jLabelImgNext.setIcon(null);
                jLabelImgNext.setText("No data");
            }else{
                try {
                    BufferedImage newImg = ImageIO.read(imageList.get(imageIndex + 1));
//                    imageBuffer.setNextBF(newImg);
//                    int imgWidth = newImg.getWidth();
//                    int imgHeight = newImg.getHeight();
                    
                    int imgWidth = newImg.getWidth();
                    int imgHeight = newImg.getHeight();

                    if(imgWidth * imgHeight <= maxImgSize){
                        imageBuffer.setNextBF(newImg);
                    }else{
                        if(imgWidth >= imgHeight){
                            width = maxImgSide;
                            height = (int)((((double)width) / imgWidth) * imgHeight);
                        }
                        else{
                            height = maxImgSide;
                            width = (int)((((double)height) / imgHeight) * imgWidth);              
                        }

                        BufferedImage imgBFnew = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                        Graphics2D g = imgBFnew.createGraphics();
                        g.drawImage(newImg, 0, 0, width, height, null);
                        g.dispose();
                        imageBuffer.setNextBF(imgBFnew);
                    }

                    if(imgWidth >= imgHeight){
                        width = jLabelImgNext.getWidth();
                        height = (int)((((double)width) / imgWidth) * imgHeight);
                    }
                    else{
                        height = jLabelImgNext.getHeight();
                        width = (int)((((double)height) / imgHeight) * imgWidth);              
                    }

                    imgBFNext = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                    Graphics2D g = imgBFNext.createGraphics();
                    g.drawImage(newImg, 0, 0, width, height, null);
                    g.dispose();

                    jLabelImgNext.setText("");
                    jLabelImgNext.setIcon(new ImageIcon(imgBFNext));
                } catch (IOException ex) {
                    Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IOException(ex);
                }
            }             
        }
        setListModel();
    }
    
    /**
     * display previous preview image
     */
    public void prev() throws IOException{
        if(imageIndex > 0){
            imageIndex--;
            imgBFNext = imgBF;
            imgBF = imgBFPrev;    
            
            imageBuffer.setNextBF(imageBuffer.getCurrentBF());
            imageBuffer.setCurrentBF(imageBuffer.getPreviousBF());
            
            /*set previous image preview*/
            if(imageIndex - 1 >= 0){
                try {                
                    BufferedImage newImg = ImageIO.read(imageList.get(imageIndex - 1));
//                    imageBuffer.setPreviousBF(newImg);
//                    int imgWidth = newImg.getWidth();
//                    int imgHeight = newImg.getHeight();
                    
                    int imgWidth = newImg.getWidth();
                    int imgHeight = newImg.getHeight();

                    if(imgWidth * imgHeight <= maxImgSize){
                        imageBuffer.setPreviousBF(newImg);
                    }else{

                        if(imgWidth >= imgHeight){
                            width = maxImgSide;
                            height = (int)((((double)width) / imgWidth) * imgHeight);
                        }
                        else{
                            height = maxImgSide;
                            width = (int)((((double)height) / imgHeight) * imgWidth);              
                        }

                        BufferedImage imgBFnew = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                        Graphics2D g = imgBFnew.createGraphics();
                        g.drawImage(newImg, 0, 0, width, height, null);
                        g.dispose();
                        imageBuffer.setPreviousBF(imgBFnew);
                    }

                    if(imgWidth >= imgHeight){
                        width = jLabelImgPrev.getWidth();
                        height = (int)((((double)width) / imgWidth) * imgHeight);
                    }
                    else{
                        height = jLabelImgPrev.getHeight();
                        width = (int)((((double)height) / imgHeight) * imgWidth);              
                    }

                    imgBFPrev = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);  
                    Graphics2D g = imgBFPrev.createGraphics();
                    g.drawImage(newImg, 0, 0, width, height, null);
                    g.dispose();

                    jLabelImgPrev.setText("");
                    jLabelImgPrev.setIcon(new ImageIcon(imgBFPrev));              
                } catch (IOException ex) {
                    Logger.getLogger(ImageHolder.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IOException(ex);
                }
            }else{
                imgBFPrev = null;
                jLabelImgPrev.setIcon(null);
                jLabelImgPrev.setText("No data");
            }

            /*set present preview image*/             
            jLabelImgCurrent.setText("");
            jLabelImgCurrent.setIcon(new ImageIcon(imgBF));

            /*set next preview image*/
            if(imageIndex == imageList.size() - 1){
                imgBFNext = null;
                jLabelImgNext.setIcon(null);
                jLabelImgNext.setText("No data");
            }else{
                jLabelImgNext.setText("");
                jLabelImgNext.setIcon(new ImageIcon(imgBFNext));         
            }
        }
        setListModel();
    }
    
    /**
     * show new names of image
     */
    private void setListModel(){
        if(imageIndex - 1 >= 0){
            jListPrev.setModel(new javax.swing.AbstractListModel() {
            String[] strings = getNewNamesFromMap(imageIndex - 1);
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }else{
            jListPrev.setModel(new javax.swing.AbstractListModel() {
            String[] strings = new String[0];
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }
        
        if(imageIndex < imageList.size()){
            jListCurrent.setModel(new javax.swing.AbstractListModel() {
            String[] strings = getNewNamesFromMap(imageIndex);
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }else{
            jListCurrent.setModel(new javax.swing.AbstractListModel() {
            String[] strings = new String[0];
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }
        
        if(imageIndex < imageList.size() - 1){
            jListNext.setModel(new javax.swing.AbstractListModel() {
            String[] strings = getNewNamesFromMap(imageIndex + 1);
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }else{
            jListNext.setModel(new javax.swing.AbstractListModel() {
            String[] strings = new String[0];
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
            });
        }        
    }
    
    /**
     * return new names from imageRenameMap
     * @param in
     * @return 
     */
    private String[] getNewNamesFromMap(int in){
        ArrayList<String> list = imageRenameMap.get(imageList.get(in));
        if(list != null){
            return list.toArray(new String[list.size()]);
        }
        return new String[0];
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelImgPrev = new javax.swing.JLabel();
        jLabelImgCurrent = new javax.swing.JLabel();
        jLabelImgNext = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListPrev = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListNext = new javax.swing.JList();
        jScrollPane4 = new javax.swing.JScrollPane();
        jListCurrent = new javax.swing.JList();

        setBackground(new java.awt.Color(245, 245, 245));
        setMaximumSize(new java.awt.Dimension(333, 190));
        setMinimumSize(new java.awt.Dimension(333, 190));
        setPreferredSize(new java.awt.Dimension(333, 190));

        jLabelImgPrev.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelImgPrev.setText("No data");
        jLabelImgPrev.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        jLabelImgPrev.setDoubleBuffered(true);

        jLabelImgCurrent.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelImgCurrent.setText("No data");
        jLabelImgCurrent.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        jLabelImgCurrent.setDoubleBuffered(true);

        jLabelImgNext.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelImgNext.setText("No data");
        jLabelImgNext.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        jLabelImgNext.setDoubleBuffered(true);

        jListPrev.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jListPrev.setEnabled(false);
        jListPrev.setVisibleRowCount(3);
        jScrollPane1.setViewportView(jListPrev);

        jListNext.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jListNext.setEnabled(false);
        jListNext.setVisibleRowCount(3);
        jScrollPane2.setViewportView(jListNext);

        jListCurrent.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jListCurrent.setEnabled(false);
        jListCurrent.setVisibleRowCount(3);
        jScrollPane4.setViewportView(jListCurrent);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelImgPrev, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelImgCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelImgNext, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelImgPrev, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelImgNext, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelImgCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelImgCurrent;
    private javax.swing.JLabel jLabelImgNext;
    private javax.swing.JLabel jLabelImgPrev;
    private javax.swing.JList jListCurrent;
    private javax.swing.JList jListNext;
    private javax.swing.JList jListPrev;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    // End of variables declaration//GEN-END:variables
}
