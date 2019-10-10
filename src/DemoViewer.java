import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class DemoViewer {
	@SuppressWarnings("serial")
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout());

		// slider to control horizontal rotation
		JSlider headingSlider = new JSlider(0, 360, 180);
		pane.add(headingSlider, BorderLayout.SOUTH);

		// slider to control vertical rotation
		JSlider pitchSlider = new JSlider(javax.swing.SwingConstants.VERTICAL, -90, 90, 0);
		pane.add(pitchSlider, BorderLayout.EAST);

		// panel to display render results
		JPanel renderPanel = new JPanel() {
			@SuppressWarnings("unchecked")
			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setColor(java.awt.Color.BLACK);
				g2.fillRect(0, 0, getWidth(), getHeight());

				// rendering will happen here
				List<Triangle> tris = new ArrayList<>();
				// A
				tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100),
						new Vertex(-100, 100, -100), Color.WHITE));
				// B
				tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100),
						new Vertex(100, -100, -100), Color.RED));
				// C
				tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100),
						new Vertex(100, 100, 100), Color.GREEN));
				// D
				tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100),
						new Vertex(-100, -100, 100), Color.BLUE));

//				g2.translate(getWidth() / 2, getHeight() / 2);
//				g2.setColor(Color.WHITE);
//				for (Triangle t : tris) {
//					java.awt.geom.Path2D path = new Path2D.Double();
//					path.moveTo(t.v1.x, t.v1.y);
//					path.lineTo(t.v2.x, t.v2.y);
//					path.lineTo(t.v3.x, t.v3.y);
//					path.closePath();
//					g2.draw(path);
//				}

				// XZ rotation matrix
				double heading = Math.toRadians(headingSlider.getValue());
				Matrix3 transform = new Matrix3(new double[] { Math.cos(heading), 0, -Math.sin(heading), 0, 1, 0,
						Math.sin(heading), 0, Math.cos(heading) });

				Matrix3 headingTransform = new Matrix3(new double[] { Math.cos(heading), 0, Math.sin(heading), 0, 1, 0,
						-Math.sin(heading), 0, Math.cos(heading) });

				// YZ rotation matrix
				double pitch = Math.toRadians(pitchSlider.getValue());
				Matrix3 pitchTransform = new Matrix3(new double[] { 1, 0, 0, 0, Math.cos(pitch), Math.sin(pitch), 0,
						-Math.sin(pitch), Math.cos(pitch) });
				transform = headingTransform.multiply(pitchTransform);

				// Creates wireframe of shape
//				g2.translate(getWidth() / 2, getHeight() / 2);
//				g2.setColor(Color.WHITE);
//				for (Triangle t : tris) {
//					Vertex v1 = transform.tranform(t.v1);
//					Vertex v2 = transform.tranform(t.v2);
//					Vertex v3 = transform.tranform(t.v3);
//					Path2D path = new Path2D.Double();
//					path.moveTo(v1.x, v1.y);
//					path.lineTo(v2.x, v2.y);
//					path.lineTo(v3.x, v3.y);
//					path.closePath();
//					g2.draw(path);
//				}

				// Rasterizes triangle - converts it to list of pixels on screen it occupies
				BufferedImage img = new BufferedImage(getWidth(), getHeight(),
						java.awt.image.BufferedImage.TYPE_INT_ARGB);

				double[] zBuffer = new double[img.getWidth() * img.getHeight()];
				// init array with extremely far away depths
				for (int q = 0; q < zBuffer.length; q++) {
					zBuffer[q] = Double.NEGATIVE_INFINITY;
				}

				for (Triangle t : tris) {
					Vertex v1 = transform.tranform(t.v1);
					Vertex v2 = transform.tranform(t.v2);
					Vertex v3 = transform.tranform(t.v3);

					// Since we are not using Graphics2D
					// We have to translate manually
					v1.x += getWidth() / 2;
					v1.y += getHeight() / 2;
					v2.x += getWidth() / 2;
					v2.y += getHeight() / 2;
					v3.x += getWidth() / 2;
					v3.y += getHeight() / 2;

					// Computes rectangular bounds for triangle
					int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
					int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
					int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
					int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

					double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
					for (int y = minY; y <= maxY; y++) {
						for (int x = minX; x <= maxX; x++) {
							double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
							double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
							double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
							if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
								double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
								int zIndex = y * img.getWidth() + x;
								if (zBuffer[zIndex] < depth) {
									img.setRGB(x, y, t.color.getRGB());
									zBuffer[zIndex] = depth;
								}
							}
						}
					}

				}

				g2.drawImage(img, 0, 0, null);
			}
		};
		pane.add(renderPanel, BorderLayout.CENTER);

		// Listeners for heading and pitch sliders to redraw
		headingSlider.addChangeListener(e -> renderPanel.repaint());
		pitchSlider.addChangeListener(e -> renderPanel.repaint());

		frame.setSize(400, 400);
		frame.setVisible(true);
	}

	static class Triangle {
		Vertex v1;
		Vertex v2;
		Vertex v3;
		Color color;

		Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.color = color;
		}
	}
}
