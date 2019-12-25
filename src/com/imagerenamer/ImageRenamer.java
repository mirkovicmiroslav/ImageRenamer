/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.imagerenamer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author Easy13
 */
public class ImageRenamer extends javax.swing.JFrame {
	private ImageBuffer imageBuffer;
	private ArrayList<File> imageList;
	private ArrayList<File> renamedFilesList;
	private HashMap<File, ArrayList<String>> imageToRenameMap;
	private HashMap<File, ArrayList<String>> renamedImageMap;
	private ArrayList<Integer> keyPressedHolder;
	private ArrayList<File> filesWithEmptyNames;
	private HashMap<File, ArrayList<String>> reviewImageMap;
	int jSpinnerValue;
	boolean showDialog;
	/*
	 * using to change old key file on a new in imageToRenameMap, for availability
	 * newNames list
	 */
	private HashMap<HashMap<File, File>, ArrayList<String>> renamedMap_OldNewFileMap_plus_NewNamesList;
	private int userNum;
	private int caretPositionBefore;
	private boolean autoSave;
	private boolean showFileName;

	/**
	 * Creates new form ImageRenamerJFrame
	 */
	public ImageRenamer() {
		imageBuffer = new ImageBuffer();

		readProperty();
		initComponents();

		imageList = new ArrayList<File>();
		imageToRenameMap = new HashMap<File, ArrayList<String>>();
		renamedFilesList = new ArrayList<File>();
		renamedMap_OldNewFileMap_plus_NewNamesList = new HashMap<HashMap<File, File>, ArrayList<String>>();
		renamedImageMap = new HashMap<File, ArrayList<String>>();
		keyPressedHolder = new ArrayList<Integer>();
		filesWithEmptyNames = new ArrayList<>();
		reviewImageMap = new HashMap<>();
		showDialog = true;

		/* Session restore dialog */
		if (new File("tmpSession.ser").exists()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					callRestoreSessionDialog();
				}
			}).start();
		}

		/* disable CTRL + V */
		KeyStroke ks = KeyStroke.getKeyStroke("control V");
		jTextFieldNewName.getInputMap().put(ks, "");
	}

	/**
	 * save program session into history file
	 */
	private void saveState() {
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;

		try {
			fileOut = new FileOutputStream("tmpSession.ser");
			out = new ObjectOutputStream(fileOut);
			out.writeObject(imageList);
			out.writeObject(imageToRenameMap);
			out.writeObject(renamedFilesList);
			out.writeObject(renamedMap_OldNewFileMap_plus_NewNamesList);
			out.writeObject(renamedImageMap);
			out.writeObject(new Integer(imageHolder.getImageIndex()));
		} catch (IOException ex) {
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			if (fileOut != null) {
				try {
					fileOut.close();
				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * restore program session from history file
	 */
	private void restoreState() {
		FileInputStream fileIn = null;
		ObjectInputStream in = null;

		try {
			fileIn = new FileInputStream("tmpSession.ser");
			in = new ObjectInputStream(fileIn);
			imageList = (ArrayList<File>) in.readObject();
			imageToRenameMap = (HashMap<File, ArrayList<String>>) in.readObject();
			renamedFilesList = (ArrayList<File>) in.readObject();
			renamedMap_OldNewFileMap_plus_NewNamesList = (HashMap<HashMap<File, File>, ArrayList<String>>) in
					.readObject();
			renamedImageMap = (HashMap<File, ArrayList<String>>) in.readObject();
			int index = (Integer) in.readObject();

			boolean rPIH = prevImageHolder.restore(index, imageList, imageToRenameMap);
			boolean rIH = imageHolder.restore(index, imageList);

			if (rPIH && rIH) {
				if (index > 0 && !imageToRenameMap.values().isEmpty()) {
					enableButtonSave(true);
				}

				setLabelFileName();
				updateImagesLeftField();
				jTextFieldNewName.setText("");
				jTextFieldNewName.grabFocus();
			} else {
				resetSession();
				JOptionPane.showMessageDialog(this, "Error recovery session", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} catch (IOException ex) {
			Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			if (fileIn != null) {
				try {
					fileIn.close();
				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * reset session
	 */
	private void resetSession() {
		setImagesList(new File[0]);

		enableButtonPrev(false);
		enableButtonNext(false);
		enableButtonRL(false);
		enableButtonRR(false);
		enableButtonsZomm(false);
		enableButtonSave(false);
		enableNewNameField(false);

		File file = new File("tmpSession.ser");
		file.setWritable(true);
		file.delete();
	}

	/**
	 * write data in property file
	 */
	private void writeProperty() {
		Properties prop = new Properties();
		OutputStream output = null;

		try {
			output = new FileOutputStream("user.properties");
			// set the properties value
			prop.setProperty("num", String.valueOf(userNum));
			prop.setProperty("autosave", String.valueOf(autoSave));
			prop.setProperty("showFileName", String.valueOf(showFileName));
			// save properties to project root folder
			prop.store(output, null);
		} catch (IOException io) {
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
				}
			}

		}
	}

	/**
	 * read data from property file
	 */
	private void readProperty() {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("user.properties");
			/* load a properties file */
			prop.load(input);

			/* get property value */
			userNum = Integer.valueOf(prop.getProperty("num"));
			autoSave = Boolean.valueOf(prop.getProperty("autosave"));
			showFileName = Boolean.valueOf(prop.getProperty("showFileName"));
		} catch (IOException ex) {
			userNum = 1;
			autoSave = true;
			showFileName = true;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Load images from folder set image list in imageHolder
	 * 
	 * @param files
	 */
	private void setImagesList(File[] files) {
		imageList.clear();
		imageToRenameMap.clear();
		renamedFilesList.clear();
		renamedMap_OldNewFileMap_plus_NewNamesList.clear();
		renamedImageMap.clear();

		for (File f : files) {
			imageList.add(f);
		}

		Collections.sort(imageList, new Comparator<File>() {
			private String str1, str2;
			private int pos1, pos2, len1, len2;

			@Override
			public int compare(File f1, File f2) {
				str1 = f1.getName();
				str2 = f2.getName();
				len1 = str1.length();
				len2 = str2.length();
				pos1 = pos2 = 0;

				int result = 0;
				while (result == 0 && pos1 < len1 && pos2 < len2) {
					char ch1 = str1.charAt(pos1);
					char ch2 = str2.charAt(pos2);

					if (Character.isDigit(ch1)) {
						result = Character.isDigit(ch2) ? compareNumbers() : -1;
					} else if (Character.isLetter(ch1)) {
						result = Character.isLetter(ch2) ? compareOther(true) : 1;
					} else {
						result = Character.isDigit(ch2) ? 1 : Character.isLetter(ch2) ? -1 : compareOther(false);
					}
					pos1++;
					pos2++;
				}

				return result == 0 ? len1 - len2 : result;
			}

			private int compareNumbers() {
				int end1 = pos1 + 1;
				while (end1 < len1 && Character.isDigit(str1.charAt(end1))) {
					end1++;
				}

				int fullLen1 = end1 - pos1;
				while (pos1 < end1 && str1.charAt(pos1) == '0') {
					pos1++;
				}

				int end2 = pos2 + 1;
				while (end2 < len2 && Character.isDigit(str2.charAt(end2))) {
					end2++;
				}

				int fullLen2 = end2 - pos2;
				while (pos2 < end2 && str2.charAt(pos2) == '0') {
					pos2++;
				}

				int delta = (end1 - pos1) - (end2 - pos2);
				if (delta != 0) {
					return delta;
				}

				while (pos1 < end1 && pos2 < end2) {
					delta = str1.charAt(pos1++) - str2.charAt(pos2++);
					if (delta != 0) {
						return delta;
					}
				}

				pos1--;
				pos2--;

				return fullLen2 - fullLen1;
			}

			private int compareOther(boolean isLetters) {
				char ch1 = str1.charAt(pos1);
				char ch2 = str2.charAt(pos2);

				if (ch1 == ch2) {
					return 0;
				}

				if (isLetters) {
					ch1 = Character.toUpperCase(ch1);
					ch2 = Character.toUpperCase(ch2);
					if (ch1 != ch2) {
						ch1 = Character.toLowerCase(ch1);
						ch2 = Character.toLowerCase(ch2);
					}
				}

				return ch1 - ch2;
			}
		});

		/* set preview images */
		/* set images for image holder */
		prevImageHolder.setImageList(imageList, imageToRenameMap);
		imageHolder.setImagesList(imageList);

		setLabelFileName();
		updateImagesLeftField();
		jTextFieldNewName.setText("");
		jTextFieldNewName.grabFocus();
		enableButtonPrev(false);
		enableButtonSave(false);

		saveState();
	}

	/**
	 * rename file by rename source called from rename button or quick key
	 */
	private void renameByChangeName(File file, ArrayList<String> newNames) {
		if (newNames.size() > 0) {
			String filePath = file.getPath().substring(0, file.getPath().lastIndexOf(File.separator));
			String fileExtension = (file.getName().substring(file.getName().lastIndexOf('.'), file.getName().length()))
					.toLowerCase();
			int addToName = 0;
			String fileName;
			String zeroNum = "";

			File renamedFile;
			int indexOfFile;

			if (renamedImageMap.containsKey(file) && renamedImageMap.get(file).contains(newNames.get(0))) {
				renamedFile = file;
			} else {
				/**
				 * if file marked lost
				 */
				if (newNames.get(0).equals("lost")) {
					File lostDir = new File(filePath + File.separator + "lost");
					if (!lostDir.exists()) {
						lostDir.mkdir();
					}

					if (userNum == 1) {
						do {
							addToName++;
							fileName = lostDir.getPath() + File.separator + newNames.get(0) + "_" + addToName
									+ fileExtension;
						} while (imageList.contains(new File(fileName)) || renamedFilesList.contains(new File(fileName))
								|| Arrays.asList(lostDir.listFiles()).contains(new File(fileName)));
					} else {
						do {
							addToName++;
							fileName = lostDir.getPath() + File.separator + newNames.get(0) + "_" + userNum
									+ ((addToName > 9) ? (String.valueOf(addToName)) : ("0" + addToName))
									+ fileExtension;
						} while (imageList.contains(new File(fileName)) || renamedFilesList.contains(new File(fileName))
								|| Arrays.asList(lostDir.listFiles()).contains(new File(fileName)));
					}

					renamedFile = new File(fileName);

					/* getting index of file in imageList */
					indexOfFile = imageList.indexOf(file);

					/* renaming old file on a new name */
					try {
						copyFile(file, renamedFile);
					} catch (IOException ex) {
						Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
					}
					file.setWritable(true);
					file.delete();
				} else {
					/**
					 * if file marked discard
					 */
					if (newNames.get(0).equals("discard")) {
						File discardDir = new File(filePath + File.separator + "discard");
						if (!discardDir.exists()) {
							discardDir.mkdir();
						}

						for (int i = 0; i < userNum; i++) {
							zeroNum += 0;
						}
						do {
							addToName++;
							fileName = discardDir.getPath() + File.separator + newNames.get(0) + "." + zeroNum
									+ addToName + fileExtension;
						} while (imageList.contains(new File(fileName)) || renamedFilesList.contains(new File(fileName))
								|| Arrays.asList(discardDir.listFiles()).contains(new File(fileName)));

						renamedFile = new File(fileName);

						/* getting index of file in imageList */
						indexOfFile = imageList.indexOf(file);

						/* renaming old file on a new name */
						try {
							copyFile(file, renamedFile);
						} catch (IOException ex) {
							Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
						}
						file.setWritable(true);
						file.delete();
					} else {
						/**
						 * if file marked promo
						 */
						if (newNames.get(0).equals("promo")) {
							File promoDir = new File(filePath + File.separator + "promo");
							if (!promoDir.exists()) {
								promoDir.mkdir();
							}

							for (int i = 0; i < userNum; i++) {
								zeroNum += 0;
							}
							do {
								addToName++;
								fileName = promoDir.getPath() + File.separator + newNames.get(0) + "." + zeroNum
										+ addToName + fileExtension;
							} while (imageList.contains(new File(fileName))
									|| renamedFilesList.contains(new File(fileName))
									|| Arrays.asList(promoDir.listFiles()).contains(new File(fileName)));

							renamedFile = new File(fileName);

							/* getting index of file in imageList */
							indexOfFile = imageList.indexOf(file);

							/* renaming old file on a new name */
							try {
								copyFile(file, renamedFile);
							} catch (IOException ex) {
								Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
							}
							file.setWritable(true);
							file.delete();
						} else {
							/**
							 * if file only renamed
							 */
							for (int i = 0; i < userNum; i++) {
								zeroNum += 0;
							}
							do {
								addToName++;
								fileName = filePath + File.separator + newNames.get(0) + "." + zeroNum + addToName
										+ fileExtension;
							} while (imageList.contains(new File(fileName))
									|| renamedFilesList.contains(new File(fileName)));

							renamedFile = new File(fileName);

							/* getting index of file in imageList */
							indexOfFile = imageList.indexOf(file);

							/* renaming old file on a new name */
//							file.renameTo(renamedFile);
						}
					}
				}

				/**
				 * replacing the old file in a imageList on a new adding renamedFile in
				 * renamedFiles list adding renamedFile and old file and theirs new names in
				 * renamedMap_OldNewFileMap_plus_NewNamesList HashMap adding renamedFile and
				 * newNames list in renamedImageMap
				 */
				imageList.set(indexOfFile, renamedFile);
				renamedFilesList.add(renamedFile);
				HashMap<File, File> map = new HashMap<File, File>();
				map.put(file, renamedFile);
				renamedMap_OldNewFileMap_plus_NewNamesList.put(map, newNames);

				ArrayList<String> nameLSforRenamedImageMap = new ArrayList<String>();
				nameLSforRenamedImageMap.add(newNames.get(0));
				renamedImageMap.put(renamedFile, nameLSforRenamedImageMap);
			}

			/* making copy of file */
//			for (int i = 1; i < newNames.size(); i++) {
//				if (!renamedImageMap.get(renamedFile).contains(newNames.get(i))) {
//					addToName = 0;
//					fileName = null;
//					zeroNum = "";
//
//					for (int j = 0; j < userNum; j++) {
//						zeroNum += 0;
//					}
//					do {
//						addToName++;
//						fileName = filePath + File.separator + newNames.get(i) + "." + zeroNum + addToName
//								+ fileExtension;
//					} while (imageList.contains(new File(fileName)) || renamedFilesList.contains(new File(fileName)));
//
//					try {
//						copyFile(renamedFile, new File(fileName));
//
//						ArrayList<String> nameLSforRenamedImageMap = renamedImageMap.get(renamedFile);
//						nameLSforRenamedImageMap.add(newNames.get(i));
//						renamedImageMap.put(renamedFile, nameLSforRenamedImageMap);
//					} catch (IOException ex) {
//						Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
//					}
//				}
//			}
		}
	}

	/**
	 * @param source
	 * @param dest
	 * @throws IOException make copy of source file to destination file
	 */
	private void copyFile(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		dest.createNewFile();
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			os.flush();
		} finally {
			is.close();
			os.close();
		}
		renamedFilesList.add(dest);
	}

	/**
	 * show Error message when program can't read file
	 */
	private void callFileErrorDialog() {
		resetSession();
		JOptionPane.showMessageDialog(this,
				"File system error.\nProbably working files was changed not through the program", "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * show Error message when program can't read file
	 */
	private void callSaveProposeDialog() {
		Object[] options = { "No, I will save it later!", "Yes, please" };
		int n = JOptionPane.showOptionDialog(this, "Would you like to save changes?", "Save", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if (n == 1)
			save();
	}

	private void callRestoreSessionDialog() {
		Object[] options = { "No", "Yes, continue session" };
		int n = JOptionPane.showOptionDialog(this, "Found an unfinished session, you want to continue?",
				"Restore session", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		if (n == 1) {
			restoreState();
		} else {
			File file = new File("tmpSession.ser");
			file.setWritable(true);
			file.delete();
		}
	}

//	private boolean reviewOfEmptyNames() {
//		for (Entry<String, String> m : reviewRenamedImages.entrySet()) {
//			do {
//
//			} while (m.getValue().isEmpty());
//		}
//		return true;
//	}

	/**
	 * Save all names in LinkedList in right format, at the end sort by names and
	 * append it to the file in right order
	 */
	private void save() {
		List<String> lines = new LinkedList<>();
		String line = null;
		for (Entry<File, ArrayList<String>> n : reviewImageMap.entrySet()) {
			String text = n.getValue().stream().map(Object::toString).collect(Collectors.joining(","));
			line = n.getKey().getName() + ":" + text;
			lines.add(line);
		}
		Collections.sort(lines);
		Charset utf8 = StandardCharsets.UTF_8;
		try {
			Files.write(Paths.get("App.index"), lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}

		enableButtonSave(false);
		saveState();

		setImagesList(new File[0]);
		File file = new File("tmpSession.ser");
		file.setWritable(true);
		file.delete();
	}

	private void nextImg() {
		if (imageHolder.getImageIndex() != imageList.size()) {
			if (imageHolder.getImageIndex() != imageList.size() - 1) {
				ArrayList<String> newNames = new ArrayList<String>();
				for (String str : jTextFieldNewName.getText().split(",")) {
					if (!str.equals("")) {
						newNames.add(str.trim());
					}
				}

				if ((imageToRenameMap.get(imageList.get(imageHolder.getImageIndex())) == null && !newNames.isEmpty())
						|| (imageToRenameMap.get(imageList.get(imageHolder.getImageIndex())) != null
								&& !imageToRenameMap.get(imageList.get(imageHolder.getImageIndex()))
										.equals(newNames))) {
					enableButtonSave(true);
				}

				if (!newNames.isEmpty()) {
					reviewImageMap.put(imageList.get(imageHolder.getImageIndex()), newNames);
				}

				imageToRenameMap.put(imageList.get(imageHolder.getImageIndex()), newNames);
				try {
					prevImageHolder.next();
					imageHolder.nextImage();
					saveState();
				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
					callFileErrorDialog();
				}

				newNames = imageToRenameMap.get(imageList.get(imageHolder.getImageIndex()));
				if (newNames != null) {
					if (!newNames.isEmpty()) {
						String names = "";
						for (int i = 0; i < newNames.size() - 1; i++) {
							names += newNames.get(i) + ",";
						}
						names += newNames.get(newNames.size() - 1);

						jTextFieldNewName.setText(names);
					} else {
						jTextFieldNewName.setText("");
					}
				} else {
					jTextFieldNewName.setText("");
				}
				jTextFieldNewName.grabFocus();
				setLabelFileName();
				updateImagesLeftField();
			} else {
				ArrayList<String> newNames = new ArrayList<String>();
				for (String str : jTextFieldNewName.getText().split(",")) {
					if (!str.equals("")) {
						newNames.add(str.trim());
					}
				}
				imageToRenameMap.put(imageList.get(imageHolder.getImageIndex()), newNames);
				if (!newNames.isEmpty()) {
					reviewImageMap.put(imageList.get(imageHolder.getImageIndex()), newNames);
				}

				try {
					prevImageHolder.next();
					imageHolder.reachedEnd();

					jTextFieldNewName.setText("");
					jTextFieldNewName.grabFocus();

					setLabelFileName();
					updateImagesLeftField();
					enableButtonSave(true);
					enableButtonNext(false);
					enableButtonRL(false);
					enableButtonRR(false);
					enableButtonsZomm(false);

					JPanel panel = new JPanel();
					panel.add(new JLabel("Choose number"));
					SpinnerNumberModel spinnerModel = new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1),
							Integer.valueOf(6), Integer.valueOf(1));
					JSpinner spinnerNumber = new JSpinner(spinnerModel);
					panel.add(spinnerNumber);

					if (showDialog) {
						JOptionPane.showOptionDialog(null, panel, "Review", JOptionPane.PLAIN_MESSAGE,
								JOptionPane.QUESTION_MESSAGE, null, null, null);
						jSpinnerValue = (int) spinnerNumber.getValue();
						showDialog = false;
					}

					saveState();
					filesWithEmptyNames.clear();
					for (Entry<File, ArrayList<String>> m : imageToRenameMap.entrySet()) {
						for (String number : m.getValue()) {
							if (number.equals("promo") || number.equals("discard") || number.equals("m")
									|| number.equals("lost") || number.equals("f")) {
								continue;
							}
							if (Pattern.compile("\\w+\\.?").matcher(number).matches()
									|| Pattern.compile("[0-9].*[a-zA-Z]").matcher(number).matches()
									|| number.length() > jSpinnerValue) {
								filesWithEmptyNames.add(m.getKey());
							}
						}
						if (m.getValue().isEmpty()) {
							filesWithEmptyNames.add(m.getKey());
						}
					}

					if (!filesWithEmptyNames.isEmpty()) {
						imageList = filesWithEmptyNames;
						Collections.sort(imageList);

						imageToRenameMap.clear();

						prevImageHolder.setImageList(imageList, imageToRenameMap);
						imageHolder.setImagesList(imageList);

						setLabelFileName();
						updateImagesLeftField();
						jTextFieldNewName.setText("");
						jTextFieldNewName.grabFocus();
						enableButtonPrev(false);
						enableButtonSave(false);
					} else {
						if (!autoSave) {
							callSaveProposeDialog();
						} else {
							save();
							imageList.clear();
							imageToRenameMap.clear();
							renamedFilesList.clear();
							renamedImageMap.clear();
							showDialog = true;
						}

					}

				} catch (IOException ex) {
					Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
					callFileErrorDialog();
				}

			}
		}
	}

	private void prevImg() {
		if (imageHolder.getImageIndex() != 0) {
			ArrayList<String> newNames;
			if (imageHolder.getImageIndex() != imageList.size()) {
				newNames = new ArrayList<String>();
				for (String str : jTextFieldNewName.getText().split(",")) {
					if (!str.equals("")) {
						newNames.add(str.trim());
					}
				}

				if (imageToRenameMap.get(imageList.get(imageHolder.getImageIndex())) != null) {
					if (!imageToRenameMap.get(imageList.get(imageHolder.getImageIndex())).equals(newNames)) {
						enableButtonSave(true);
					}
				}

				imageToRenameMap.put(imageList.get(imageHolder.getImageIndex()), newNames);
			}
			try {
				prevImageHolder.prev();
				imageHolder.prevImage();
			} catch (IOException ex) {
				Logger.getLogger(ImageRenamer.class.getName()).log(Level.SEVERE, null, ex);
				callFileErrorDialog();
			}

			jTextFieldNewName.grabFocus();

			newNames = imageToRenameMap.get(imageList.get(imageHolder.getImageIndex()));
			if (newNames != null) {
				if (!newNames.isEmpty()) {
					String names = "";
					for (int i = 0; i < newNames.size() - 1; i++) {
						names += newNames.get(i) + ",";
					}
					names += newNames.get(newNames.size() - 1);
					jTextFieldNewName.setText(names);
				} else {
					jTextFieldNewName.setText("");
				}
			} else {
				jTextFieldNewName.setText("");
			}

			setLabelFileName();
			updateImagesLeftField();
			enableButtonNext(true);
			enableButtonRL(true);
			enableButtonRR(true);
			enableButtonsZomm(true);
		}
		saveState();
	}

	private void zoomInImg() {
		if (imageHolder.getImageIndex() != imageList.size()) {
			imageHolder.zoomIn();
			jTextFieldNewName.grabFocus();
		}
	}

	private void zoomOutImg() {
		if (imageHolder.getImageIndex() != imageList.size()) {
			imageHolder.zoomOut();
			jTextFieldNewName.grabFocus();
		}
	}

	private void rotateR() {
		if (imageHolder.getImageIndex() != imageList.size()) {
			imageHolder.rotateRight();
			jTextFieldNewName.grabFocus();
		}
	}

	private void rotateL() {
		if (imageHolder.getImageIndex() != imageList.size()) {
			imageHolder.rotateLeft();
			jTextFieldNewName.grabFocus();
		}
	}

	private void moveImageLeft() {
		imageHolder.moveImgLeft();
	}

	private void moveImageRight() {
		imageHolder.moveImgRight();
	}

	private void moveImageUp() {
		imageHolder.moveImgUp();
	}

	private void moveImageDown() {
		imageHolder.moveImgDown();
	}

	private void setLabelFileName() {
		if (imageHolder.getImageIndex() < imageList.size()) {
			jLabelFileName.setText(imageList.get(imageHolder.getImageIndex()).getName());
		} else {
			jLabelFileName.setText("");
		}
	}

	private void updateImagesLeftField() {
		jTextFieldImagesLeft.setText(String.valueOf(imageList.size() - imageHolder.getImageIndex()));
	}

	public void enableButtonNext(boolean b) {
		jButtonNextImg.setEnabled(b);
	}

	public void enableButtonPrev(boolean b) {
		jButtonPrevImg.setEnabled(b);
	}

	public void enableButtonRR(boolean b) {
		jButtonRotateR.setEnabled(b);
	}

	public void enableButtonRL(boolean b) {
		jButtonRotateL.setEnabled(b);
	}

	public void enableButtonSave(boolean b) {
		jButtonSave.setEnabled(b);
	}

	public void enableButtonsZomm(boolean b) {
		jButtonZoomIn.setEnabled(b);
		jButtonZoomOut.setEnabled(b);
	}

	public void enableNewNameField(boolean b) {
		jTextFieldNewName.setEnabled(b);
	}

//    InputVerifier inf = new InputVerifier() {
//
//        @Override
//        public boolean verify(JComponent input) {
//            javax.swing.JSpinner spinner = (javax.swing.JSpinner) input;
//
//            if((int)spinner.getValue() > 0) {
//                return true;
//            }else {
//                spinner.setValue(1);
//                return false;
//            }
//        }
//    };

	/**
	 * This method is called from within the constructor to initialize the form.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jFileChooser = new javax.swing.JFileChooser();
		jDialogSettings = new javax.swing.JDialog();
		jLabelDialogSetNum = new javax.swing.JLabel();
		jButtonDialogOk = new javax.swing.JButton();
		jButtonDialogCancel = new javax.swing.JButton();
		jSpinnerDialogSetNum = new javax.swing.JSpinner();
		jLabelAutoSave = new javax.swing.JLabel();
		jRadioButtonAutoSaveYes = new javax.swing.JRadioButton();
		jRadioButtonAutoSaveNo = new javax.swing.JRadioButton();
		jCheckBoxShowFileName = new javax.swing.JCheckBox();
		buttonGroupAutoSave = new javax.swing.ButtonGroup();
		jScrollPaneMain = new javax.swing.JScrollPane();
		jPanelMain = new javax.swing.JPanel();
		jLabelImagesLeft = new javax.swing.JLabel();
		jTextFieldImagesLeft = new javax.swing.JTextField();
		jButtonNextImg = new javax.swing.JButton();
		jButtonPrevImg = new javax.swing.JButton();
		jButtonRotateR = new javax.swing.JButton();
		jButtonRotateL = new javax.swing.JButton();
		jTextFieldNewName = new javax.swing.JTextField();
		jButtonSave = new javax.swing.JButton();
		jButtonZoomIn = new javax.swing.JButton();
		jButtonZoomOut = new javax.swing.JButton();
		imageHolder = new com.imagerenamer.ImageHolder();
		prevImageHolder = new com.imagerenamer.PrevImageHolder();
		jLabelFile = new javax.swing.JLabel();
		jLabelFileName = new javax.swing.JLabel();
		jMenuBar = new javax.swing.JMenuBar();
		jMenuFile = new javax.swing.JMenu();
		jMenuItemLoadImages = new javax.swing.JMenuItem();
		jMenuItemExit = new javax.swing.JMenuItem();
		jMenuTools = new javax.swing.JMenu();
		jMenuItemSettings = new javax.swing.JMenuItem();

		jFileChooser.setCurrentDirectory(
				new java.io.File("/home/vadim/C:/Users/Vadim/Desktop/Race.Photos.2.Practice - ÐºÐ¾Ð¿Ð¸Ñ�"));
		jFileChooser.setMultiSelectionEnabled(true);

		jDialogSettings.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		jDialogSettings.setMinimumSize(new java.awt.Dimension(365, 215));
		jDialogSettings.setResizable(false);

		jLabelDialogSetNum.setText("Set #");

		jButtonDialogOk.setText("Ok");
		jButtonDialogOk.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDialogOkActionPerformed(evt);
			}
		});

		jButtonDialogCancel.setText("Cancel");
		jButtonDialogCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDialogCancelActionPerformed(evt);
			}
		});

		jSpinnerDialogSetNum.setModel(
				new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
		jSpinnerDialogSetNum.setValue(userNum);

		jLabelAutoSave.setText("Autosave when reach the end");

		buttonGroupAutoSave.add(jRadioButtonAutoSaveYes);
		jRadioButtonAutoSaveYes.setSelected(autoSave);
		jRadioButtonAutoSaveYes.setText("Yes");

		buttonGroupAutoSave.add(jRadioButtonAutoSaveNo);
		jRadioButtonAutoSaveNo.setSelected(!autoSave);
		jRadioButtonAutoSaveNo.setText("No, I'll self decide when to save the changes");

		jCheckBoxShowFileName.setSelected(showFileName);
		jCheckBoxShowFileName.setText("Show file name");

		javax.swing.GroupLayout jDialogSettingsLayout = new javax.swing.GroupLayout(jDialogSettings.getContentPane());
		jDialogSettings.getContentPane().setLayout(jDialogSettingsLayout);
		jDialogSettingsLayout.setHorizontalGroup(jDialogSettingsLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jDialogSettingsLayout.createSequentialGroup().addGroup(jDialogSettingsLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(18, 18, 18)
								.addComponent(jLabelDialogSetNum)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jSpinnerDialogSetNum, javax.swing.GroupLayout.PREFERRED_SIZE, 62,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(18, 18, 18)
								.addComponent(jLabelAutoSave))
						.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(20, 20, 20)
								.addGroup(jDialogSettingsLayout
										.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(jRadioButtonAutoSaveNo).addComponent(jRadioButtonAutoSaveYes)))
						.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(18, 18, 18)
								.addComponent(jButtonDialogOk).addGap(18, 18, 18).addComponent(jButtonDialogCancel))
						.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(18, 18, 18)
								.addComponent(jCheckBoxShowFileName)))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		jDialogSettingsLayout.setVerticalGroup(jDialogSettingsLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jDialogSettingsLayout.createSequentialGroup().addGap(12, 12, 12)
						.addGroup(jDialogSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabelDialogSetNum).addComponent(jSpinnerDialogSetNum,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
						.addComponent(jLabelAutoSave)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jRadioButtonAutoSaveYes)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jRadioButtonAutoSaveNo)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
						.addComponent(jCheckBoxShowFileName)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
						.addGroup(jDialogSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jButtonDialogOk).addComponent(jButtonDialogCancel))
						.addGap(24, 24, 24)));

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setBackground(new java.awt.Color(255, 255, 255));
		setMinimumSize(new java.awt.Dimension(400, 300));

		jScrollPaneMain.setBackground(new java.awt.Color(255, 255, 255));
		jScrollPaneMain.setBorder(null);

		jPanelMain.setBackground(new java.awt.Color(245, 245, 245));
		jPanelMain.setPreferredSize(new java.awt.Dimension(800, 600));

		jLabelImagesLeft.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
		jLabelImagesLeft.setText("Images left:");

		jTextFieldImagesLeft.setEditable(false);
		jTextFieldImagesLeft.setBackground(new java.awt.Color(245, 245, 245));
		jTextFieldImagesLeft.setText("0");
		jTextFieldImagesLeft.setBorder(null);

		Image imgNext;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/right_ICO_BLUE.png"));
			imgNext = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonNextImg.setIcon(new javax.swing.ImageIcon(imgNext));
		} catch (Throwable ex) {
			jButtonNextImg.setText("Next");
		}
		jButtonNextImg.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonNextImg.setEnabled(false);
		jButtonNextImg.setFocusable(false);
		jButtonNextImg.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonNextImg.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonNextImg.setNextFocusableComponent(jButtonPrevImg);
		jButtonNextImg.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonNextImg.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonNextImgMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonNextImgMouseReleased(evt);
			}
		});
		jButtonNextImg.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonNextImgActionPerformed(evt);
			}
		});

		Image imgPrev;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/left_ICO_BLUE.png"));
			imgPrev = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonPrevImg.setIcon(new javax.swing.ImageIcon(imgPrev));
		} catch (Throwable ex) {
			jButtonPrevImg.setText("Prev");
		}
		jButtonPrevImg.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonPrevImg.setEnabled(false);
		jButtonPrevImg.setFocusable(false);
		jButtonPrevImg.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonPrevImg.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonPrevImg.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonPrevImg.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonPrevImgMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonPrevImgMouseReleased(evt);
			}
		});
		jButtonPrevImg.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonPrevImgActionPerformed(evt);
			}
		});

		Image imgRR;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/rotateRight_ICO_BLUE.png"));
			imgRR = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonRotateR.setIcon(new javax.swing.ImageIcon(imgRR));
		} catch (Throwable ex) {
			jButtonRotateR.setText("RR");
		}
		jButtonRotateR.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonRotateR.setEnabled(false);
		jButtonRotateR.setFocusable(false);
		jButtonRotateR.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonRotateR.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonRotateR.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonRotateR.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonRotateRMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonRotateRMouseReleased(evt);
			}
		});
		jButtonRotateR.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonRotateRActionPerformed(evt);
			}
		});

		Image imgRL;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/rotateLeft_ICO_BLUE.png"));
			imgRL = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonRotateL.setIcon(new javax.swing.ImageIcon(imgRL));
		} catch (Throwable ex) {
			jButtonRotateL.setText("RL");
		}
		jButtonRotateL.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonRotateL.setEnabled(false);
		jButtonRotateL.setFocusable(false);
		jButtonRotateL.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonRotateL.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonRotateL.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonRotateL.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonRotateLMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonRotateLMouseReleased(evt);
			}
		});
		jButtonRotateL.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonRotateLActionPerformed(evt);
			}
		});

		jTextFieldNewName.setEnabled(false);
		jTextFieldNewName.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyPressed(java.awt.event.KeyEvent evt) {
				jTextFieldNewNameKeyPressed(evt);
			}

			public void keyReleased(java.awt.event.KeyEvent evt) {
				jTextFieldNewNameKeyReleased(evt);
			}
		});

		Image imgSave;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/save_ICO_BLUE.png"));
			imgSave = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonSave.setIcon(new javax.swing.ImageIcon(imgSave));
		} catch (Throwable ex) {
			jButtonSave.setText("Save");
		}
		jButtonSave.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonSave.setEnabled(false);
		jButtonSave.setFocusable(false);
		jButtonSave.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonSave.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonSave.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonSave.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonSaveMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonSaveMouseReleased(evt);
			}
		});
		jButtonSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonSaveActionPerformed(evt);
			}
		});

		Image imgZoomIN;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/in_ICO_BLUE.png"));

			imgZoomIN = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonZoomIn.setIcon(new javax.swing.ImageIcon(imgZoomIN));
		} catch (Throwable ex) {
			jButtonZoomIn.setText("IN");
		}
		jButtonZoomIn.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonZoomIn.setEnabled(false);
		jButtonZoomIn.setFocusable(false);
		jButtonZoomIn.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonZoomIn.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonZoomIn.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonZoomIn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonZoomInMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonZoomInMouseReleased(evt);
			}
		});
		jButtonZoomIn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonZoomInActionPerformed(evt);
			}
		});

		Image imgZoomOUT;
		try {
			BufferedImage imgBF = ImageIO.read(getClass().getResource("/images/out_ICO_BLUE.png"));
			imgZoomOUT = imgBF.getScaledInstance(35, 35, Image.SCALE_SMOOTH);
			jButtonZoomOut.setIcon(new javax.swing.ImageIcon(imgZoomOUT));
		} catch (Throwable ex) {
			jButtonZoomOut.setText("OUT");
		}
		jButtonZoomOut.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		jButtonZoomOut.setEnabled(false);
		jButtonZoomOut.setFocusable(false);
		jButtonZoomOut.setMaximumSize(new java.awt.Dimension(35, 35));
		jButtonZoomOut.setMinimumSize(new java.awt.Dimension(35, 35));
		jButtonZoomOut.setPreferredSize(new java.awt.Dimension(35, 35));
		jButtonZoomOut.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jButtonZoomOutMousePressed(evt);
			}

			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButtonZoomOutMouseReleased(evt);
			}
		});
		jButtonZoomOut.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonZoomOutActionPerformed(evt);
			}
		});
		imageHolder.setParentForm(this);
		imageHolder.setBuffer(imageBuffer);

		prevImageHolder.setBuffer(imageBuffer);

		jLabelFile.setText("File:");
		jLabelFile.setVisible(showFileName);

		jLabelFileName.setVisible(showFileName);

		javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
		jPanelMainLayout.setHorizontalGroup(jPanelMainLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(jPanelMainLayout.createSequentialGroup().addContainerGap()
						.addComponent(imageHolder, GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(jPanelMainLayout.createSequentialGroup().addComponent(jLabelImagesLeft)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(jTextFieldImagesLeft,
												GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE))
								.addComponent(prevImageHolder, GroupLayout.PREFERRED_SIZE, 331,
										GroupLayout.PREFERRED_SIZE)
								.addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING, false)
										.addGroup(jPanelMainLayout.createSequentialGroup().addComponent(jLabelFile)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(jLabelFileName, GroupLayout.DEFAULT_SIZE,
														GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
										.addGroup(jPanelMainLayout.createSequentialGroup().addGroup(jPanelMainLayout
												.createParallelGroup(Alignment.LEADING)
												.addGroup(jPanelMainLayout.createSequentialGroup()
														.addComponent(jButtonPrevImg, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(jButtonNextImg, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addGap(42)
														.addComponent(jButtonRotateL, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(jButtonRotateR, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
												.addComponent(jTextFieldNewName, GroupLayout.PREFERRED_SIZE, 194,
														GroupLayout.PREFERRED_SIZE))
												.addGap(42)
												.addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
														.addComponent(jButtonSave, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addGroup(jPanelMainLayout.createSequentialGroup()
																.addComponent(jButtonZoomOut,
																		GroupLayout.PREFERRED_SIZE,
																		GroupLayout.DEFAULT_SIZE,
																		GroupLayout.PREFERRED_SIZE)
																.addPreferredGap(ComponentPlacement.RELATED)
																.addComponent(jButtonZoomIn, GroupLayout.PREFERRED_SIZE,
																		GroupLayout.DEFAULT_SIZE,
																		GroupLayout.PREFERRED_SIZE))))))
						.addContainerGap()));
		jPanelMainLayout.setVerticalGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(jPanelMainLayout.createSequentialGroup().addContainerGap().addGroup(jPanelMainLayout
						.createParallelGroup(
								Alignment.LEADING)
						.addGroup(jPanelMainLayout.createSequentialGroup()
								.addComponent(
										prevImageHolder, GroupLayout.PREFERRED_SIZE, 182, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
										.addComponent(jButtonNextImg, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(jButtonPrevImg, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(jButtonRotateL, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(jButtonRotateR, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(jButtonZoomIn, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(jButtonZoomOut, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGap(18)
								.addGroup(jPanelMainLayout.createParallelGroup(Alignment.TRAILING)
										.addComponent(jButtonSave, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(jTextFieldNewName, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGap(18)
								.addGroup(jPanelMainLayout.createParallelGroup(Alignment.BASELINE)
										.addComponent(jLabelFile).addComponent(jLabelFileName,
												GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(ComponentPlacement.RELATED, 299, Short.MAX_VALUE)
								.addGroup(jPanelMainLayout.createParallelGroup(Alignment.BASELINE)
										.addComponent(jLabelImagesLeft)
										.addComponent(jTextFieldImagesLeft, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
						.addComponent(imageHolder, GroupLayout.DEFAULT_SIZE, 623, Short.MAX_VALUE)).addContainerGap()));

		jPanelMain.setLayout(jPanelMainLayout);

		jScrollPaneMain.setViewportView(jPanelMain);
		jPanelMain.getAccessibleContext().setAccessibleName("");

		jMenuFile.setText("File");

		jMenuItemLoadImages.setText("Load images");
		jMenuItemLoadImages.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemLoadImagesActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemLoadImages);

		jMenuItemExit.setText("Exit");
		jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemExitActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemExit);

		jMenuBar.add(jMenuFile);

		jMenuTools.setText("Tools");

		jMenuItemSettings.setText("Settings");
		jMenuItemSettings.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemSettingsActionPerformed(evt);
			}
		});
		jMenuTools.add(jMenuItemSettings);

		jMenuBar.add(jMenuTools);

		setJMenuBar(jMenuBar);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1021, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	/**
	 * select image list by file chooser
	 * 
	 * @param evt
	 */
	private void jMenuItemLoadImagesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemLoadImagesActionPerformed
		int fjResponce = jFileChooser.showOpenDialog(this);
		if (fjResponce == 0)
			setImagesList(jFileChooser.getSelectedFiles());
	}// GEN-LAST:event_jMenuItemLoadImagesActionPerformed

	private void jButtonNextImgActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonNextImgActionPerformed
		nextImg();
	}// GEN-LAST:event_jButtonNextImgActionPerformed

	private void jButtonPrevImgActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonPrevImgActionPerformed
		prevImg();
	}// GEN-LAST:event_jButtonPrevImgActionPerformed

	private void jButtonRotateLActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonRotateLActionPerformed
		rotateL();
	}// GEN-LAST:event_jButtonRotateLActionPerformed

	private void jButtonRotateRActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonRotateRActionPerformed
		rotateR();
	}// GEN-LAST:event_jButtonRotateRActionPerformed

	private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonSaveActionPerformed
		save();
	}// GEN-LAST:event_jButtonSaveActionPerformed

	private void jButtonZoomInActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonZoomInActionPerformed
		zoomInImg();
	}// GEN-LAST:event_jButtonZoomInActionPerformed

	private void jButtonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonZoomOutActionPerformed
		zoomOutImg();
	}// GEN-LAST:event_jButtonZoomOutActionPerformed

	private void jMenuItemSettingsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemSettingsActionPerformed
		jDialogSettings.setLocationRelativeTo(this);
		jDialogSettings.setModal(true);
		jDialogSettings.setVisible(true);
	}// GEN-LAST:event_jMenuItemSettingsActionPerformed

	private void jButtonDialogCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonDialogCancelActionPerformed
		jDialogSettings.dispose();
	}// GEN-LAST:event_jButtonDialogCancelActionPerformed

	/**
	 * set property in property file
	 * 
	 * @param evt
	 */
	private void jButtonDialogOkActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButtonDialogOkActionPerformed
		userNum = (int) jSpinnerDialogSetNum.getValue();
		autoSave = jRadioButtonAutoSaveYes.isSelected();
		showFileName = jCheckBoxShowFileName.isSelected();
		jDialogSettings.dispose();

		jLabelFile.setVisible(showFileName);
		jLabelFileName.setVisible(showFileName);
		/* write properties in property file */
		writeProperty();
	}// GEN-LAST:event_jButtonDialogOkActionPerformed

	private void jTextFieldNewNameKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_jTextFieldNewNameKeyReleased
		keyPressedHolder.remove(new Integer(evt.getKeyCode()));

		/* step to next or previous image */
		if (((Character) evt.getKeyChar()).equals('>')) {
			String names = new String(jTextFieldNewName.getText().substring(0, caretPositionBefore) + jTextFieldNewName
					.getText().substring(caretPositionBefore + 1, jTextFieldNewName.getText().length()));
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
			jTextFieldNewName.setText(names);
			nextImg();
		}
		if (((Character) evt.getKeyChar()).equals('<')) {
			String names = new String(jTextFieldNewName.getText().substring(0, caretPositionBefore) + jTextFieldNewName
					.getText().substring(caretPositionBefore + 1, jTextFieldNewName.getText().length()));
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
			jTextFieldNewName.setText(names);
			prevImg();
		}

		/**
		 * not used here, this part execute in jTextFieldNewNameKeyPressed
		 */
		/*
		 * //move scrollbar of image viewer if(evt.isControlDown() && evt.getKeyCode()
		 * if (evt.isControlDown() && ev.getKeyCode() == 37){ moveImageLeft();
		 * jTextFieldNewName.setCaretPosition(caretPositionBefore); }
		 * if(evt.isControlDown() && evt.getKeyCode() == 39){ moveImageRight();
		 * jTextFieldNewName.setCaretPosition(caretPositionBefore); }
		 * if(evt.isControlDown() && evt.getKeyCode() == 38){ moveImageUp(); }
		 * if(evt.isControlDown() && evt.getKeyCode() == 40){ moveImageDown(); }
		 */

		/* zoom IN or OUT */
		if (((Character) evt.getKeyChar()).equals('+') || ((Character) evt.getKeyChar()).equals('=')) {
			String names = new String(jTextFieldNewName.getText().substring(0, caretPositionBefore) + jTextFieldNewName
					.getText().substring(caretPositionBefore + 1, jTextFieldNewName.getText().length()));
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
			jTextFieldNewName.setText(names);
			zoomInImg();
		}
		if (((Character) evt.getKeyChar()).equals('-')) {
			String names = new String(jTextFieldNewName.getText().substring(0, caretPositionBefore) + jTextFieldNewName
					.getText().substring(caretPositionBefore + 1, jTextFieldNewName.getText().length()));
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
			jTextFieldNewName.setText(names);
			zoomOutImg();
		}

		/**
		 * not used here, this part execute in jTextFieldNewNameKeyPressed
		 */
		/*
		 * //same names if(evt.getKeyCode() == 32){ int index =
		 * imageHolder.getImageIndex() - 1; ArrayList<String> newNames =
		 * imageRenameMap.get(imageList.get(index)); if(newNames != null){
		 * if(!newNames.isEmpty()){ String names = ""; for(int i = 0; i <
		 * newNames.size() - 1; i++){ names += newNames.get(i) + ","; } names +=
		 * newNames.get(newNames.size() - 1);
		 * 
		 * jTextFieldNewName.setText(names); } }else{ jTextFieldNewName.setText(""); } }
		 */

		/* rotate image right or left */
		if (evt.getKeyCode() == 82) {
			String names = jTextFieldNewName.getText().substring(0, jTextFieldNewName.getText().length() - 1);
			jTextFieldNewName.setText(names);
			rotateR();
		}
		if (evt.getKeyCode() == 76) {
			String names = jTextFieldNewName.getText().substring(0, jTextFieldNewName.getText().length() - 1);
			jTextFieldNewName.setText(names);
			rotateL();
		}

		/* save changes */
//        if(evt.getKeyCode() == 112 && imageHolder.getImageIndex() == imageList.size()){
//            save();
//        }
		if (evt.getKeyCode() == 112 && jButtonSave.isEnabled()) {
			save();
		}

		/* set image as lost */
		if (((Character) evt.getKeyChar()).equals('x')) {
			String names = "lost";
			jTextFieldNewName.setText(names);
		}

		/* set image as discard */
		if (((Character) evt.getKeyChar()).equals('d')) {
			String names = "discard";
			jTextFieldNewName.setText(names);
		}

		/* set image as promo */
		if (((Character) evt.getKeyChar()).equals('p')) {
			String names = "promo";
			jTextFieldNewName.setText(names);
		}
	}// GEN-LAST:event_jTextFieldNewNameKeyReleased

	private void jButtonNextImgMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonNextImgMousePressed
		jButtonNextImg.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonNextImgMousePressed

	private void jButtonNextImgMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonNextImgMouseReleased
		jButtonNextImg.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonNextImgMouseReleased

	private void jButtonPrevImgMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonPrevImgMousePressed
		jButtonPrevImg.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonPrevImgMousePressed

	private void jButtonPrevImgMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonPrevImgMouseReleased
		jButtonPrevImg.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonPrevImgMouseReleased

	private void jButtonRotateLMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonRotateLMousePressed
		jButtonRotateL.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonRotateLMousePressed

	private void jButtonRotateLMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonRotateLMouseReleased
		jButtonRotateL.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonRotateLMouseReleased

	private void jButtonRotateRMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonRotateRMousePressed
		jButtonRotateR.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonRotateRMousePressed

	private void jButtonRotateRMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonRotateRMouseReleased
		jButtonRotateR.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonRotateRMouseReleased

	private void jButtonZoomInMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonZoomInMousePressed
		jButtonZoomIn.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonZoomInMousePressed

	private void jButtonZoomInMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonZoomInMouseReleased
		jButtonZoomIn.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonZoomInMouseReleased

	private void jButtonZoomOutMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonZoomOutMousePressed
		jButtonZoomOut.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonZoomOutMousePressed

	private void jButtonZoomOutMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonZoomOutMouseReleased
		jButtonZoomOut.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonZoomOutMouseReleased

	private void jButtonSaveMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonSaveMousePressed
		jButtonSave.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	}// GEN-LAST:event_jButtonSaveMousePressed

	private void jButtonSaveMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jButtonSaveMouseReleased
		jButtonSave.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
	}// GEN-LAST:event_jButtonSaveMouseReleased

	private void jTextFieldNewNameKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_jTextFieldNewNameKeyPressed
		caretPositionBefore = jTextFieldNewName.getCaretPosition();

		if (evt.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER) {
			keyPressedHolder.add(evt.getKeyCode());
		}

		if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
			nextImg();
		}

		if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) {
			nextImg();
		}

		if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
			prevImg();
		}

		/* autofill */
		if (evt.getKeyCode() == 32) {
			int index = imageHolder.getImageIndex() - 1;
			ArrayList<String> newNames = imageToRenameMap.get(imageList.get(index));
			if (newNames != null) {
				if (!newNames.isEmpty()) {
					if (newNames.contains("discard") && (index - 1 >= 0)) {
						newNames = imageToRenameMap.get(imageList.get(index - 1));
					}

					String names = "";
					for (int i = 0; i < newNames.size() - 1; i++) {
						names += newNames.get(i) + ",";
					}
					names += newNames.get(newNames.size() - 1);

					if (jTextFieldNewName.getText().equals("")) {
						jTextFieldNewName.setText(names);
					} else {
						jTextFieldNewName.setText(jTextFieldNewName.getText() + "," + names);
					}

					if (index - 1 < 0) {
						jTextFieldNewName.setText("");
					}
				}
			} else {
				jTextFieldNewName.setText("");
			}
		}

		// move scrollbar of image viewer
		if (evt.isControlDown() && evt.getKeyCode() == 37) {
			moveImageLeft();
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
		}
		if (evt.isControlDown() && evt.getKeyCode() == 39) {
			moveImageRight();
			jTextFieldNewName.setCaretPosition(caretPositionBefore);
		}
		if (evt.isControlDown() && evt.getKeyCode() == 38) {
			moveImageUp();
		}
		if (evt.isControlDown() && evt.getKeyCode() == 40) {
			moveImageDown();
		}
	}// GEN-LAST:event_jTextFieldNewNameKeyPressed

	private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemExitActionPerformed
		this.dispose();
		System.exit(0);
	}// GEN-LAST:event_jMenuItemExitActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
		// (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
		 * look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(ImageRenamer.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(ImageRenamer.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(ImageRenamer.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(ImageRenamer.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ImageRenamer().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.ButtonGroup buttonGroupAutoSave;
	private com.imagerenamer.ImageHolder imageHolder;
	private javax.swing.JButton jButtonDialogCancel;
	private javax.swing.JButton jButtonDialogOk;
	private javax.swing.JButton jButtonNextImg;
	private javax.swing.JButton jButtonPrevImg;
	private javax.swing.JButton jButtonRotateL;
	private javax.swing.JButton jButtonRotateR;
	private javax.swing.JButton jButtonSave;
	private javax.swing.JButton jButtonZoomIn;
	private javax.swing.JButton jButtonZoomOut;
	private javax.swing.JCheckBox jCheckBoxShowFileName;
	private javax.swing.JDialog jDialogSettings;
	private javax.swing.JFileChooser jFileChooser;
	private javax.swing.JLabel jLabelAutoSave;
	private javax.swing.JLabel jLabelDialogSetNum;
	private javax.swing.JLabel jLabelFile;
	private javax.swing.JLabel jLabelFileName;
	private javax.swing.JLabel jLabelImagesLeft;
	private javax.swing.JMenuBar jMenuBar;
	private javax.swing.JMenu jMenuFile;
	private javax.swing.JMenuItem jMenuItemExit;
	private javax.swing.JMenuItem jMenuItemLoadImages;
	private javax.swing.JMenuItem jMenuItemSettings;
	private javax.swing.JMenu jMenuTools;
	private javax.swing.JPanel jPanelMain;
	private javax.swing.JRadioButton jRadioButtonAutoSaveNo;
	private javax.swing.JRadioButton jRadioButtonAutoSaveYes;
	private javax.swing.JScrollPane jScrollPaneMain;
	private javax.swing.JSpinner jSpinnerDialogSetNum;
	private javax.swing.JTextField jTextFieldImagesLeft;
	private javax.swing.JTextField jTextFieldNewName;
	private com.imagerenamer.PrevImageHolder prevImageHolder;
}
