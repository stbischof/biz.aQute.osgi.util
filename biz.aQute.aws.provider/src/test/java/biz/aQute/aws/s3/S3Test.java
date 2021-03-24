package biz.aQute.aws.s3;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import aQute.lib.io.IO;
import biz.aQute.aws.credentials.UserCredentials;
import biz.aQute.aws.s3.api.Bucket;
import biz.aQute.aws.s3.api.Bucket.Content;
import junit.framework.TestCase;

public class S3Test extends TestCase {

	public void testSignedUrl() throws Exception {
		UserCredentials uc = new UserCredentials();

		S3Impl s3 = new S3Impl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey());
		deleteBucket(s3);

		BucketImpl bucket = s3.createBucket("libsync-test");
		URI signedUri = bucket.putObject("hello").contentType("application/octet-stream").signedUri(10000);
		System.out.println(signedUri);

		HttpURLConnection con = (HttpURLConnection) signedUri.toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "application/octet-stream");
		IO.copy(IO.reader("Hello world"), con.getOutputStream());
		int rsp = con.getResponseCode();
		InputStream errorStream = con.getErrorStream();
		if (errorStream != null)
			IO.copy(con.getErrorStream(), System.out);

		assertEquals(200, rsp);
	}

	public void testSimple() throws Exception {
		UserCredentials uc = new UserCredentials();

		S3Impl s3 = new S3Impl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey());
		deleteBucket(s3);

		BucketImpl bucket = s3.createBucket("libsync-test");
		try {
			assertNotNull(bucket);
			assertEquals("libsync-test", bucket.getName());
			assertFalse(bucket.listObjects().iterator().hasNext());

			boolean found = false;
			for (Bucket b : s3.listBuckets()) {
				if ("libsync-test".equals(b.getName()))
					found = true;
			}
			assertTrue(found);

			bucket.putObject("1/a").put("0123");
			bucket.putObject("2/b").put("abcdefgh");

			boolean one = false;
			boolean two = true;
			int count = 0;
			for (Content content : bucket.listObjects()) {
				count++;
				assertEquals(bucket, content.bucket);
				if ("1/a".equals(content.key)) {
					one = true;
					assertEquals("eb62f6b9306db575c2d596b1279627a4", content.etag);
					assertEquals(4, content.size);
					String c = IO.collect(bucket.getObject("1/a").get());
					assertEquals("0123", c);
				} else if ("2/b".equals(content.key)) {
					one = true;
					assertEquals("e8dc4081b13434b45189a720b77b6818", content.etag);
					assertEquals(8, content.size);
					String c = IO.collect(bucket.getObject("2/b").get());
					assertEquals("abcdefgh", c);
				} else
					fail("Unrecognized content " + content.key);
			}
			assertEquals(2, count);
			assertTrue(one);
			assertTrue(two);

			bucket.putObject("1/a").put("aa");
			bucket.delete("hullo");
			bucket.delete("bye");
		}
		finally {
			deleteBucket(s3);
		}
	}

	public void testEBR() throws Exception {
		UserCredentials uc = new UserCredentials();

		S3Impl s3 = new S3Impl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey());
		BucketImpl bucket = s3.getBucket("repository.springframework.org");
		traverse(bucket, "", "");
	}

	void traverse(BucketImpl bucket, String prefix, String indent) throws Exception {
		for (Content x : bucket.listObjects().delimiter("/")) {
			System.out.printf("%s%20s %d %s\n", indent, x.key, x.size, x.etag);
		}

	}

	private void deleteBucket(S3Impl s3) {
		try {
			BucketImpl b = s3.getBucket("libsync-test");
			if (b != null) {
				for (Content c : b.listObjects()) {
					b.delete(c.key);
				}
				s3.deleteBucket("libsync-test");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}