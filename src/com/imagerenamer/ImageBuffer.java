/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.imagerenamer;

import java.awt.image.BufferedImage;

/**
 *
 * @author Easy13
 */
public class ImageBuffer {
    private BufferedImage previousBF;
    private BufferedImage presentBF;
    private BufferedImage nextBF;
    
    public ImageBuffer(){}

    /**
     * set previous BufferedImage
     * @param previousBF 
     */
    public void setPreviousBF(BufferedImage previousBF) {
        this.previousBF = previousBF;
    }
    
    /**
     * set current BufferedImage
     * @param presentBF 
     */
    public void setCurrentBF(BufferedImage presentBF) {
        this.presentBF = presentBF;
    }

    /**
     * set next BufferedImage
     * @param nextBF 
     */
    public void setNextBF(BufferedImage nextBF) {
        this.nextBF = nextBF;
    }

    /**
     * get previous BufferedImage
     * @return 
     */
    public BufferedImage getPreviousBF() {
        return previousBF;
    }
    
    /**
     * get Current BufferedImage
     * @return 
     */
    public BufferedImage getCurrentBF() {
        return presentBF;
    }    

    /**
     * get next BufferedImage
     * @return 
     */
    public BufferedImage getNextBF() {
        return nextBF;
    }
}
