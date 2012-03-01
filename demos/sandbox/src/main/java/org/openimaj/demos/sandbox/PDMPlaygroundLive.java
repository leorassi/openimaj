/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.demos.sandbox;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openimaj.demos.sandbox.asm.AMPTSDataset;
import org.openimaj.demos.sandbox.asm.ASFDataset;
import org.openimaj.demos.sandbox.asm.ActiveShapeModel.IterationResult;
import org.openimaj.demos.sandbox.asm.MultiResolutionActiveShapeModel;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.PointDistributionModel;
import org.openimaj.math.geometry.shape.PointList;
import org.openimaj.math.geometry.shape.PointListConnections;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

import Jama.Matrix;

public class PDMPlaygroundLive {
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
//		ASFDataset dataset = new ASFDataset(new File("/Users/jsh2/Work/lmlk/trunk/shared/JAAM-API/data/face-data"));
//		AMPTSDataset dataset = new AMPTSDataset(
//				new File("/Users/jsh2/Downloads/am_tools/points"), 
//				new File("/Users/jsh2/Downloads/am_tools/images"),
//				new File("/Users/jsh2/Downloads/am_tools/models/face.parts"));
		ASFDataset dataset = new ASFDataset(new File("/Users/jsh2/Downloads/imm_face_db"));
		
		final List<IndependentPair<PointList, FImage>> data = dataset.getData();
		final PointListConnections connections = dataset.getConnections();
		
		final float scale = 0.04f;
		final MultiResolutionActiveShapeModel asm = MultiResolutionActiveShapeModel.trainModel(4, 2, 4, scale, 30, connections, data, new PointDistributionModel.EllipsoidConstraint(3));

		VideoDisplay.createVideoDisplay(new VideoCapture(320, 240))
		.addVideoListener(new VideoDisplayListener<MBFImage>() {

			HaarCascadeDetector detector = new HaarCascadeDetector(80);

			@Override
			public void beforeUpdate(MBFImage frame) {
				FImage image = frame.flatten();
				List<DetectedFace> faces = detector.detectFaces(image);

				if (faces == null || faces.size() == 0) return;

				for (DetectedFace face : faces) {
					frame.drawShape(face.getBounds(), RGBColour.GREEN);

					Point2d cog = face.getBounds().getCOG();
					double facescale = (double)face.getBounds().height / 4;

					Matrix pose = TransformUtilities.translateMatrix(cog.getX(), cog.getY()).times(TransformUtilities.scaleMatrix(facescale, facescale));
					PointList shape = asm.getPDM().getMean().transform(pose);

					long t1 = System.currentTimeMillis();
					IterationResult newData = asm.fit(image, pose, shape);
					long t2 = System.currentTimeMillis();

					shape = newData.shape;
					pose = newData.pose;

					frame.drawLines(connections.getLines(shape), 1, RGBColour.RED);

					float shapeScale = shape.computeIntrinsicScale();
					for (Point2d pt : shape) {
						Line2d normal = connections.calculateNormalLine(pt, shape, scale * shapeScale);
						if (normal != null) frame.drawLine(normal, 1, RGBColour.BLUE);
					}

					System.out.println(newData.fit);
					System.out.println(t2 - t1);
				}
			}

			@Override
			public void afterUpdate(VideoDisplay<MBFImage> display) {}
		});
	}
}