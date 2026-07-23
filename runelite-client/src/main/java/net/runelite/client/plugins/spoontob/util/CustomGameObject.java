package net.runelite.client.plugins.spoontob.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;
import net.runelite.api.GameObject;
import net.runelite.api.Model;

/**
 * Wraps a GameObject so its model's face colors can be overridden (e.g. for barrier
 * recoloring) and later restored to their original values.
 */
public class CustomGameObject
{
	private final GameObject obj;
	private final int id;

	private int[] oldColors1;
	private int[] oldColors2;
	private int[] oldColors3;

	public CustomGameObject(GameObject obj, int id)
	{
		this.obj = obj;
		this.id = id;
	}

	public void setFaceColorValues(Color color)
	{
		Model model = this.obj.getRenderable().getModel();
		if (model == null || color == null)
		{
			return;
		}

		int[] colors1 = model.getFaceColors1();
		int[] colors2 = model.getFaceColors2();
		int[] colors3 = model.getFaceColors3();

		if (isFaceColorsNullOrEmpty(this.oldColors1, this.oldColors2, this.oldColors3))
		{
			this.oldColors1 = colors1.clone();
			this.oldColors2 = colors2.clone();
			this.oldColors3 = colors3.clone();
		}

		int rs2 = ColorsUtil.RGBtoRS2HSB(color.getRed(), color.getGreen(), color.getBlue());
		for (int[] faceColors : new int[][]{colors1, colors2, colors3})
		{
			if (faceColors.length > 0)
			{
				Arrays.fill(faceColors, rs2);
			}
		}
	}

	public void restore()
	{
		Model model = this.obj.getRenderable().getModel();
		if (model == null || isFaceColorsNullOrEmpty(this.oldColors1, this.oldColors2, this.oldColors3))
		{
			return;
		}

		System.arraycopy(this.oldColors1, 0, model.getFaceColors1(), 0, this.oldColors1.length);
		System.arraycopy(this.oldColors2, 0, model.getFaceColors2(), 0, this.oldColors2.length);
		System.arraycopy(this.oldColors3, 0, model.getFaceColors3(), 0, this.oldColors3.length);
		this.oldColors1 = null;
		this.oldColors2 = null;
		this.oldColors3 = null;
	}

	private static boolean isFaceColorsNullOrEmpty(int[]... arrays)
	{
		for (int[] array : arrays)
		{
			if (array == null || array.length == 0)
			{
				return true;
			}
		}
		return false;
	}

	public GameObject getObj()
	{
		return this.obj;
	}

	public int getId()
	{
		return this.id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof CustomGameObject))
		{
			return false;
		}
		return Objects.equals(this.obj, ((CustomGameObject) o).obj);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.obj);
	}
}
