// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.photo.aspects;

import net.spy.photo.PhotoImage;
import net.spy.photo.PhotoImageData;
import net.spy.photo.ImageServer;
import net.spy.photo.observation.NewImageObservable;

/**
 * Send notifications when new images are stored.
 */
public aspect StorerNotifications {

	pointcut storedImage(PhotoImageData pid, PhotoImage image):
		execution(public void ImageServer.storeImage(PhotoImageData, PhotoImage))
		&& args(pid, image);

	after(PhotoImageData pid, PhotoImage image) returning:
		storedImage(pid, image) {

		// Let everyone know there's a new image.
		NewImageObservable.getInstance().newImage(pid, image);
	}
}
