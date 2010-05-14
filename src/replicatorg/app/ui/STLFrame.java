package replicatorg.app.ui;

import javax.swing.JFrame;

import replicatorg.model.BuildModel;

public class STLFrame extends JFrame {
	public STLFrame(final String path) {
		setTitle(path);
		setSize(400,400);
		stlPreview = new STLPreviewPanel();
		stlPreview.setModel(new BuildModel() {
			public String getSTLPath() {
				return path;
			}
		});
		initComponents();
	}
	
	private void initComponents() {

		setPreferredSize(new java.awt.Dimension(400, 400));
		getContentPane().add(stlPreview, java.awt.BorderLayout.CENTER);

		pack();
	}

	private STLPreviewPanel stlPreview;

}
