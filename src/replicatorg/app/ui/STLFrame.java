package replicatorg.app.ui;

import javax.swing.JFrame;

public class STLFrame extends JFrame {
	public STLFrame(String path) {
		setTitle(path);
		setSize(400,400);
		stlPreview = new STLPreviewPanel(path);
		initComponents();
	}
	
	private void initComponents() {

		setPreferredSize(new java.awt.Dimension(400, 400));
		getContentPane().add(stlPreview, java.awt.BorderLayout.CENTER);

		pack();
	}

	private STLPreviewPanel stlPreview;

}
