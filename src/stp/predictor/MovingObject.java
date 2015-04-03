package stp.predictor;


public class MovingObject implements Comparable<MovingObject> {
	/**
	 * unique id
	 */
	public int id;

	/**
	 * last update time
	 */
	public long t;

	/**
	 * location at t
	 */
	public Point position;

	/**
	 * velocity at t
	 */
	public Point velocity;

	/**
	 * Create an object
	 * 
	 * @param id
	 *            unique id of the object
	 * @param pos
	 *            position at t
	 * @param vel
	 *            velocity at t
	 * @param t
	 *            reported update time t (usually current time)
	 */
	public MovingObject(int id, Point pos, Point vel, long t) {
		this.id = id;
		this.t = t;
		position = pos;
		velocity = vel;
	}

	/**
	 * Constructor: Clone from input object
	 * 
	 * @param obj
	 */
	public MovingObject(MovingObject obj) {
		this.id = obj.id;
		this.t = obj.t;
		position = new Point(obj.position);
		velocity = new Point(obj.velocity);
	}

	/**
	 * Constructor
	 */
	public MovingObject() {
		position = new Point();
		velocity = new Point();
	}

	/**
	 * Set the stored information to time t. Update location and last update
	 * time accordingly.
	 * 
	 * @param t
	 *            new update time (usually current time)
	 */
	public void set_time(long t) {
		position.x += velocity.x * (t - this.t);
		position.y += velocity.y * (t - this.t);
		this.t = t;
	}

	/**
	 * Get object (predicative) location at time t
	 * 
	 * @param t
	 *            the predicative time
	 * @return location at time t
	 */
	public Point get_position_at_time(long t) {
		double px = position.x + velocity.x * (t - this.t);
		double py = position.y + velocity.y * (t - this.t);
		return new Point(px, py);
	}

	/**
	 * Get the (predicative) distance to another object at time t
	 * 
	 * @param obj
	 *            the other object
	 * @param t
	 *            the predictive time
	 * @return the distance
	 */
	public double get_distance_at_time(MovingObject obj, long t) {
		Point p1 = this.get_position_at_time(t);
		Point p2 = obj.get_position_at_time(t);
		return p1.distance(p2);
	}

	/**
	 * Create an object from an input string
	 * 
	 * @param line
	 *            input string
	 * @return a new object
	 */
	public static MovingObject parse(String line) {
		String[] args = line.split("\t");
		int id = Integer.parseInt(args[0].trim());
		Point position = new Point(Double.parseDouble(args[1].trim()), Double
				.parseDouble(args[2].trim()));
		Point velocity = new Point(Double.parseDouble(args[3].trim()), Double
				.parseDouble(args[4].trim()));
		long t = Long.parseLong(args[5].trim());

		return new MovingObject(id, position, velocity, t);
	}

	/**
	 * Return a string representing the object
	 */
	public String toString() {
		return id + "\t" + position.x + "\t" + position.y + "\t" + velocity.x
				+ "\t" + velocity.y + "\t" + t;
	}

	public MovingObject(String line) {
		String delimitor = " ";
		if (line.contains("\t"))
			delimitor = "\t";

		String[] str = line.split(delimitor);
		id = Integer.parseInt(str[0]);
		position = new Point(Double.parseDouble(str[1]), Double
				.parseDouble(str[2]));
		velocity = new Point(Double.parseDouble(str[3]), Double
				.parseDouble(str[4]));
		t = Long.parseLong(str[5]);
	}

	@Override
	public int compareTo(MovingObject o) {
		if (t < o.t)
			return -1;
		if (t > o.t)
			return 1;
		return 0;
	}

}
