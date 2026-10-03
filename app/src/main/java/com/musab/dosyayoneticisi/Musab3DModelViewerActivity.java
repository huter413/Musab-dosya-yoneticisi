package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Musab3DModelViewerActivity extends Activity {
    private ModelView view;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,8,10));
        getWindow().setNavigationBarColor(Color.rgb(8,8,10));
        String path=getIntent().getStringExtra("path");
        if(path==null) { finish(); return; }
        FrameLayoutCompat root=new FrameLayoutCompat(this);
        view=new ModelView(this,path);
        root.addView(view);
        TextView title=new TextView(this);
        title.setText("Musab 3D Model Görüntüleyici");
        title.setTextColor(Color.WHITE); title.setTextSize(17); title.setPadding(18,14,18,14);
        title.setBackgroundColor(Color.argb(190,15,15,18));
        root.addView(title,new android.widget.FrameLayout.LayoutParams(-1,60));
        setContentView(root);
    }
    @Override public void onBackPressed(){ if(view!=null&&view.resetGesture()){return;} super.onBackPressed(); }

    static class FrameLayoutCompat extends android.widget.FrameLayout {
        FrameLayoutCompat(Context c){super(c);}
    }

    static class ModelView extends GLSurfaceView {
        final ViewerRenderer renderer;
        float lastX,lastY,lastDist,lastMidX,lastMidY;
        int mode=0;
        ModelView(Context c,String path){
            super(c); setEGLContextClientVersion(2);
            renderer=new ViewerRenderer(path);
            setRenderer(renderer); setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            setBackgroundColor(Color.rgb(8,8,10));
        }
        boolean resetGesture(){return false;}
        float dist(MotionEvent e){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);return (float)Math.hypot(dx,dy);}
        float midX(MotionEvent e){return (e.getX(0)+e.getX(1))*0.5f;}
        float midY(MotionEvent e){return (e.getY(0)+e.getY(1))*0.5f;}
        @Override public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();mode=1;return true;}
            if(a==MotionEvent.ACTION_POINTER_DOWN&&e.getPointerCount()>=2){
                lastDist=dist(e);lastMidX=midX(e);lastMidY=midY(e);mode=2;return true;
            }
            if(a==MotionEvent.ACTION_MOVE){
                if(e.getPointerCount()>=2){
                    float d=dist(e),mx=midX(e),my=midY(e);
                    if(lastDist>0) renderer.distance*=Math.pow(lastDist/Math.max(1,d),1.0);
                    renderer.distance=Math.max(0.08f,Math.min(100f,renderer.distance));
                    renderer.panX+=(mx-lastMidX)*renderer.distance/Math.max(1,getWidth())*2.2f;
                    renderer.panY-=(my-lastMidY)*renderer.distance/Math.max(1,getHeight())*2.2f;
                    lastDist=d;lastMidX=mx;lastMidY=my;requestRender();
                } else if(mode==1){
                    float dx=e.getX()-lastX,dy=e.getY()-lastY;
                    renderer.yaw+=dx*0.55f; renderer.pitch+=dy*0.55f;
                    renderer.pitch=Math.max(-89f,Math.min(89f,renderer.pitch));
                    lastX=e.getX();lastY=e.getY();requestRender();
                }
                return true;
            }
            if(a==MotionEvent.ACTION_POINTER_UP){ if(e.getPointerCount()-1<2) mode=1; return true; }
            if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){mode=0;return true;}
            return true;
        }
    }

    static class ViewerRenderer implements GLSurfaceView.Renderer {
        final String path; volatile float[] verts; volatile int count;
        float yaw=35,pitch=-15,distance=3,panX=0,panY=0;
        int program,vbo; float[] model=new float[16],view=new float[16],proj=new float[16],mvp=new float[16];
        ViewerRenderer(String p){path=p;new Thread(()->load()).start();}
        void load(){try{Mesh m=ModelLoader.load(new File(path));verts=m.vertices;count=verts.length/6;}catch(Exception e){verts=new float[0];count=0;}}
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig c){
            GLES20.glClearColor(0.035f,0.035f,0.045f,1);
            String vs="attribute vec3 aPos;attribute vec3 aNor;uniform mat4 uMvp;varying vec3 vN;void main(){vN=aNor;gl_Position=uMvp*vec4(aPos,1.0);}";
            String fs="precision mediump float;varying vec3 vN;void main(){vec3 n=normalize(vN);float l=max(0.18,dot(n,normalize(vec3(0.5,0.8,1.0))));vec3 base=vec3(0.62,0.72,0.86);gl_FragColor=vec4(base*(0.35+0.75*l),1.0);}";
            program=link(vs,fs);vbo=GLES20.glGenBuffers(1,new int[]{0},0);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
        int link(String v,String f){int a=shader(GLES20.GL_VERTEX_SHADER,v),b=shader(GLES20.GL_FRAGMENT_SHADER,f),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,a);GLES20.glAttachShader(p,b);GLES20.glLinkProgram(p);return p;}
        int shader(int t,String s){int x=GLES20.glCreateShader(t);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x;}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.setLookAtM(view,0,0,0,5,0,0,0,0,1,0);Matrix.perspectiveM(proj,0,55f,(float)w/Math.max(1,h),0.01f,200f);}
        @Override public void onDrawFrame(GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            if(count==0)return;
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,panX,panY,0);
            Matrix.rotateM(model,0,pitch,1,0,0); Matrix.rotateM(model,0,yaw,0,1,0);
            Matrix.setLookAtM(view,0,0,0,distance,0,0,0,1,0);
            float[] mv=new float[16];Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvp,0,proj,0,mv,0);
            FloatBuffer b=ByteBuffer.allocateDirect(verts.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(verts).position(0);
            GLES20.glUseProgram(program);int p=GLES20.glGetAttribLocation(program,"aPos"),n=GLES20.glGetAttribLocation(program,"aNor"),u=GLES20.glGetUniformLocation(program,"uMvp");
            GLES20.glUniformMatrix4fv(u,1,false,mvp,0);b.position(0);GLES20.glEnableVertexAttribArray(p);GLES20.glVertexAttribPointer(p,3,GLES20.GL_FLOAT,false,24,b);
            b.position(3);GLES20.glEnableVertexAttribArray(n);GLES20.glVertexAttribPointer(n,3,GLES20.GL_FLOAT,false,24,b);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
        }
    }

    static class Mesh {float[] vertices; Mesh(float[] v){vertices=v;}}
    static class ModelLoader {
        static Mesh load(File f)throws Exception{
            String n=f.getName().toLowerCase(Locale.ROOT);
            if(n.endsWith(".obj"))return obj(f);
            if(n.endsWith(".stl"))return stl(f);
            if(n.endsWith(".ply"))return ply(f);
            throw new IOException("Desteklenen 3D formatı: OBJ, STL veya PLY");
        }
        static Mesh obj(File f)throws Exception{
            ArrayList<float[]> v=new ArrayList<>(),out=new ArrayList<>();BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
            String s;while((s=r.readLine())!=null){s=s.trim();if(s.startsWith("v ")){String[] a=s.substring(2).trim().split("\\s+");v.add(new float[]{Float.parseFloat(a[0]),Float.parseFloat(a[1]),Float.parseFloat(a[2])});}
                else if(s.startsWith("f ")){String[] a=s.substring(2).trim().split("\\s+");if(a.length>=3)for(int i=1;i<a.length-1;i++){int[] ids={idx(a[0]),idx(a[i]),idx(a[i+1])};float[] A=v.get(ids[0]),B=v.get(ids[1]),C=v.get(ids[2]);float[] no=normal(A,B,C);for(int id:ids){float[] P=v.get(id);for(float q:P)out.add(q);for(float q:no)out.add(q);}}}}
            r.close();float[] z=new float[out.size()];for(int i=0;i<z.length;i++)z[i]=out.get(i)[0];return new Mesh(normalize(z));
        }
        static int idx(String s){return Integer.parseInt(s.split("/")[0])-1;}
        static Mesh stl(File f)throws Exception{
            ArrayList<Float> o=new ArrayList<>();byte[] h=new byte[80];DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(f)));in.readFully(h);int tri=Integer.reverseBytes(in.readInt());long expected=84L+50L*tri;
            if(f.length()==expected){for(int t=0;t<tri;t++){float nx=Float.intBitsToFloat(Integer.reverseBytes(in.readInt())),ny=Float.intBitsToFloat(Integer.reverseBytes(in.readInt())),nz=Float.intBitsToFloat(Integer.reverseBytes(in.readInt()));for(int k=0;k<3;k++){for(int j=0;j<3;j++){float q=Float.intBitsToFloat(Integer.reverseBytes(in.readInt()));o.add(q);}o.add(nx);o.add(ny);o.add(nz);}in.readUnsignedShort();}in.close();}
            else{in.close();BufferedReader r=new BufferedReader(new FileReader(f));ArrayList<float[]> vs=new ArrayList<>();String s;while((s=r.readLine())!=null){s=s.trim();if(s.startsWith("vertex ")){String[] a=s.substring(7).trim().split("\\s+");vs.add(new float[]{Float.parseFloat(a[0]),Float.parseFloat(a[1]),Float.parseFloat(a[2])});if(vs.size()==3){float[] no=normal(vs.get(0),vs.get(1),vs.get(2));for(float[] p:vs){for(float q:p)o.add(q);for(float q:no)o.add(q);}vs.clear();}}}r.close();}
            float[] z=new float[o.size()];for(int i=0;i<z.length;i++)z[i]=o.get(i);return new Mesh(normalize(z));
        }
        static Mesh ply(File f)throws Exception{
            BufferedReader r=new BufferedReader(new FileReader(f));String s;int nv=0,nf=0;boolean head=true;while(head&&(s=r.readLine())!=null){if(s.startsWith("element vertex"))nv=Integer.parseInt(s.replaceAll("\\D+",""));else if(s.startsWith("element face"))nf=Integer.parseInt(s.replaceAll("\\D+",""));else if(s.equals("end_header"))head=false;}
            ArrayList<float[]> v=new ArrayList<>();for(int i=0;i<nv;i++){String[] a=r.readLine().trim().split("\\s+");v.add(new float[]{Float.parseFloat(a[0]),Float.parseFloat(a[1]),Float.parseFloat(a[2])});}
            ArrayList<Float> o=new ArrayList<>();for(int i=0;i<nf;i++){String[] a=r.readLine().trim().split("\\s+");int c=Integer.parseInt(a[0]);for(int k=1;k<c-1;k++){int[] ids={Integer.parseInt(a[1]),Integer.parseInt(a[k+1]),Integer.parseInt(a[k+2])};float[] no=normal(v.get(ids[0]),v.get(ids[1]),v.get(ids[2]));for(int id:ids){for(float q:v.get(id))o.add(q);for(float q:no)o.add(q);}}}r.close();float[] z=new float[o.size()];for(int i=0;i<z.length;i++)z[i]=o.get(i);return new Mesh(normalize(z));
        }
        static float[] normal(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];float x=uy*vz-uz*vy,y=uz*vx-ux*vz,z=ux*vy-uy*vx,l=(float)Math.sqrt(x*x+y*y+z*z);return l<1e-6f?new float[]{0,0,1}:new float[]{x/l,y/l,z/l};}
        static float[] normalize(float[] a){if(a.length==0)return a;float minx=Float.MAX_VALUE,miny=minx,minz=minx,maxx=-minx,maxy=-minx,maxz=-minx;for(int i=0;i<a.length;i+=6){minx=Math.min(minx,a[i]);maxx=Math.max(maxx,a[i]);miny=Math.min(miny,a[i+1]);maxy=Math.max(maxy,a[i+1]);minz=Math.min(minz,a[i+2]);maxz=Math.max(maxz,a[i+2]);}float cx=(minx+maxx)/2,cy=(miny+maxy)/2,cz=(minz+maxz)/2,scale=Math.max(maxx-minx,Math.max(maxy-miny,maxz-minz));scale=scale<1e-6?1:2/scale;for(int i=0;i<a.length;i+=6){a[i]=(a[i]-cx)*scale;a[i+1]=(a[i+1]-cy)*scale;a[i+2]=(a[i+2]-cz)*scale;}return a;}
    }
}
