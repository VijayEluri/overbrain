

import java.io.*;
import java.net.*;
import java.util.*;

import state.Quadtree;

import ciberIF.*;


/**
 * example of a basic agent
 * implemented using the java interface library.
 */
public class GetTrainingData {
	ciberIF cif;
	private String robName;
	private double irSensor0, irSensor1, irSensor2, compass;
	private beaconMeasure beacon;
	private int    ground;
	private boolean collision;
	private double x,y,dir;
	private double left, right;

	private int beaconToFollow;
	private double oldDir;
	private double oldAngleDiff;
	private double savedTime;
	BufferedWriter out;
	Random r;

	public static void main(String[] args) {
		String host, robName;
		int pos; 
		int arg;

		//default values
		host = "localhost";
		robName = "jClient";
		pos = 1;


		// parse command-line arguments
		try {
			arg = 0;
			while (arg<args.length) {
				if(args[arg].equals("-pos")) {
					if(args.length > arg+1) {
						pos = Integer.valueOf(args[arg+1]).intValue();
						arg += 2;
					}
				}
				else if(args[arg].equals("-robname")) {
					if(args.length > arg+1) {
						robName = args[arg+1];
						arg += 2;
					}
				}
				else if(args[arg].equals("-host")) {
					if(args.length > arg+1) {
						host = args[arg+1];
						arg += 2;
					}
				}
				else throw new Exception();
			}
		}
		catch (Exception e) {
			print_usage();
			return;
		}
		


		/*
		 * READ THE MAP AND MAKE THE PLAN
		 */
//		ReadXmlMap a = new ReadXmlMap(args[0], args[1]);
//		Planner p = new Planner(a.getQuadtree(), a.getStart(0), a.getTarget() , 0.5);
//		Vector<Quadtree> path = p.aStar();

		// create client
		GetTrainingData client = new GetTrainingData();

		client.robName = robName;

		// register robot in simulator
		client.cif.InitRobot(robName, pos, host);

		// main loop
		client.mainLoop(null);

	}

	// Constructor
	GetTrainingData() {
		cif = new ciberIF();
		beacon = new beaconMeasure();

		beaconToFollow = 0;
		ground=-1;
		left = right = 0;
		
		r = new Random();
		
		// Open File
		try{
			// Create file 
			FileWriter fstream = new FileWriter("train.txt");
			out = new BufferedWriter(fstream);
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}

	/** 
	 * reads a new message, decides what to do and sends action to simulator
	 */
	public void mainLoop (Vector<Quadtree> path) {

		while(true) {
			cif.ReadSensors();
			decide(path);
		}
	}
	
	// returns InPow in a noise free environment
	private double setEngine(double current, double effective) {
		double inPow = 2 * (effective - current);
		return inPow;
	}

	/**
	 * basic reactive decision algorithm, decides action based on current sensor values
	 */
	public void decide(Vector<Quadtree> path) {
		if(cif.GetStartButton() == false)
			return;
		
		
		
		if(cif.IsObstacleReady(0))
			irSensor0 = cif.GetObstacleSensor(0);
		if(cif.IsObstacleReady(1))
			irSensor1 = cif.GetObstacleSensor(1);
		if(cif.IsObstacleReady(2))
			irSensor2 = cif.GetObstacleSensor(2);
		if(cif.IsCompassReady())
			compass = cif.GetCompassSensor();
		if(cif.IsGroundReady())
			ground = cif.GetGroundSensor();

		if(cif.IsBeaconReady(beaconToFollow))
			beacon = cif.GetBeaconSensor(beaconToFollow);

		oldDir = dir;
		x = cif.GetX();
		y = cif.GetY();
		dir = cif.GetDir();
		if (cif.IsBumperReady())
			collision = cif.GetBumperSensor();
		

		
		if (collision || cif.GetStopButton() == true || cif.GetTime() > 10000) {
			cif.Finish();
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
		else {
			System.out.println(cif.GetTime());
			if(cif.GetTime() != savedTime) {
				double angleDif = dir - oldDir;
				oldDir = dir;
				try {
					out.write(","+angleDif+"\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					System.exit(0);
				}
				double newLeft, newRight, leftIn, rightIn;
				int signal = 1;
				
				// left engine
				do {
					if (r.nextBoolean() == true)
						signal = -1;
					else
						signal = 1;
					double n = r.nextInt(15);
					newLeft = signal * n/100;
					leftIn = setEngine(left, newLeft);
				} while (Math.abs(leftIn) > 0.15);
				//System.out.println("newLeft = " + newLeft + "leftIn = " + leftIn);
				
				// right engine
				do {
					if (r.nextBoolean() == true)
						signal = -1;
					else
						signal = 1;
					double n = r.nextInt(15);
					newRight = signal * n/100;
					rightIn = setEngine(right, newRight);
				} while (Math.abs(rightIn) > 0.15);
				//System.out.println("newRight = " + newRight + "rightIn = " + rightIn);
				
				// write to file
				String line = +left+","+right+
				","+leftIn+","+rightIn;
				System.out.println(line);
				try {
					out.write(line);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(0);
				}
				// new stuff
				cif.DriveMotors(leftIn, rightIn);
				left = newLeft;
				right = newRight;
				savedTime = cif.GetTime();
			}
			else {
				//System.out.println("WTF?");
			}
			


			

		}
		//System.out.println("Time is " + cif.GetTime());
		//System.out.println("Measures: ir0=" + irSensor0 + " ir1=" + irSensor1 + " ir2=" + irSensor2 + "\n");
		//System.out.println("Measures: x=" + x + " y=" + y + " dir=" + dir);

		//		if(ground==beaconToFollow)
		//			cif.Finish();
		//		else {
		//			if(irSensor0>4.0 || irSensor1>4.0 ||  irSensor2>4.0) 
		//				cif.DriveMotors(0.1,-0.1);
		//			else if(irSensor1>1.0) cif.DriveMotors(0.1,0.0);
		//			else if(irSensor2>1.0) cif.DriveMotors(0.0,0.1);
		//			else {
		//				double destX = path.get(0).getCenter().getX();
		//				double destY = path.get(0).getCenter().getX();
		//
		//				// calculate direction
		//				double vx = destX - x;
		//				double vy = destY - y;
		//				
		//				double destDir = Math.atan2(vy, vx);
		//				System.out.println("Objective: x=" + destX + " y=" + destY + " dir=" + destDir);
		//				// rotate
		//				double angleDiff = destDir - dir;
		//				if (angleDiff > Math.PI)
		//					angleDiff -= Math.PI * 2;
		//				
		//				if (angleDiff < -Math.PI)
		//					angleDiff += Math.PI * 2;
		//				
		//				
		//
		//				System.out.println("dir: " + dir);
		//			}
		//	        else if(beacon.beaconVisible && beacon.beaconDir > 20.0) 
		//		    cif.DriveMotors(0.0,0.1);
		//	        else if(beacon.beaconVisible && beacon.beaconDir < -20.0) 
		//		    cif.DriveMotors(0.1,0.0);
		//	        else cif.DriveMotors(0.1,0.1);
//	}

//	for(int i=0; i<5; i++)
//		if(cif.NewMessageFrom(i))
//			System.out.println("Message: From " + i + " to " + robName + " : \"" + cif.GetMessageFrom(i)+ "\"");
//
//	cif.Say(robName);

	if(cif.GetTime() % 2 == 0) {
		cif.RequestIRSensor(0);
		if(cif.GetTime() % 8 == 0 || beaconToFollow == cif.GetNumberOfBeacons())
			cif.RequestGroundSensor();
		else
			cif.RequestBeaconSensor(beaconToFollow);
	}
	else {
		cif.RequestIRSensor(1);
		cif.RequestIRSensor(2);
	}
	

}

static void print_usage() {
	System.out.println("Usage: java jClient [-robname <robname>] [-pos <pos>] [-host <hostname>[:<port>]]");
}


};

