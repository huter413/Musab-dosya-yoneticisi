const express=require("express");
const {execFile}=require("child_process");
const fs=require("fs");
const os=require("os");
const path=require("path");
const crypto=require("crypto");

const app=express();
app.use(express.json({limit:"60mb"}));
const PORT=process.env.PORT||8080;
const REPO=process.env.GITHUB_REPOSITORY||"huter413/Musab-dosya-yoneticisi";
const TOKEN=process.env.GITHUB_TOKEN;
if(!TOKEN) console.warn("GITHUB_TOKEN servis ortamında ayarlanmadı.");

app.post("/build",async(req,res)=>{
  if(!TOKEN)return res.status(503).json({state:"failed",error:"GitHub Secret GITHUB_TOKEN servis ortamına bağlanmamış."});
  const {type,sign,javaVersion,target,sourceName,sourceBase64}=req.body||{};
  if(!sourceBase64||!["APK","JAR"].includes(type))return res.status(400).json({state:"failed",error:"Geçersiz derleme isteği."});
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),"musab-build-"));
  const source=path.join(dir,sourceName||"source.zip");
  fs.writeFileSync(source,Buffer.from(sourceBase64,"base64"));
  const branch="musab-build-service-"+Date.now()+"-"+crypto.randomBytes(4).toString("hex");
  try{
    const base=await gh("/repos/"+REPO+"/git/ref/heads/main");
    await gh("/repos/"+REPO+"/git/refs","POST",{ref:"refs/heads/"+branch,sha:base.object.sha});
    await putFile("build-input/"+branch+"/config.json",Buffer.from(JSON.stringify({type,sign:!!sign,javaVersion,target,sourceName})).toString("base64"),branch);
    await putFile("build-input/"+branch+"/source"+(sourceName.toLowerCase().endsWith(".jar")?".jar":".zip"),source,branch);
    const run=await waitRun(branch);
    if(run.conclusion!=="success")return res.status(422).json({state:"failed",error:"GitHub Actions derlemesi başarısız."});
    const arts=await gh("/repos/"+REPO+"/actions/runs/"+run.id+"/artifacts");
    const art=(arts.artifacts||[]).find(x=>x.name==="musab-build-output"&&!x.expired);
    if(!art)throw new Error("Derleme çıktısı bulunamadı.");
    const zip=await raw(art.archive_download_url);
    const z=path.join(dir,"out.zip");fs.writeFileSync(z,zip);
    const {execFileSync}=require("child_process");execFileSync("unzip",["-o",z,"-d",dir]);
    const files=fs.readdirSync(dir).filter(x=>type==="APK"?x.endsWith(".apk"):x.endsWith(".jar"));
    if(!files.length)throw new Error("APK/JAR çıktısı arşivde yok.");
    const out=fs.readFileSync(path.join(dir,files[0]));
    res.json({state:"success",outputName:files[0],outputBase64:out.toString("base64")});
  }catch(e){res.status(500).json({state:"failed",error:e.message});}
  finally{fs.rmSync(dir,{recursive:true,force:true});}
});
async function gh(p,m="GET",body){return (await fetch("https://api.github.com"+p,{method:m,headers:{"Authorization":"Bearer "+TOKEN,"Accept":"application/vnd.github+json","X-GitHub-Api-Version":"2022-11-28","Content-Type":"application/json"},body:body?JSON.stringify(body):undefined})).then(async r=>{const t=await r.text();if(!r.ok)throw new Error("GitHub API "+r.status+": "+t);return JSON.parse(t);});}
async function putFile(p,b64,branch){return gh("/repos/"+REPO+"/contents/"+p,"PUT",{message:"Musab build input",content:b64,branch});}
async function waitRun(branch){for(let i=0;i<144;i++){const x=await gh("/repos/"+REPO+"/actions/runs?branch="+encodeURIComponent(branch)+"&event=push&per_page=10");const r=(x.workflow_runs||[]).find(y=>y.head_branch===branch);if(r&&r.status==="completed")return r;await new Promise(s=>setTimeout(s,5000));}throw new Error("Actions zaman aşımı.");}
async function raw(url){const r=await fetch(url,{headers:{"Authorization":"Bearer "+TOKEN,"Accept":"application/vnd.github+json"}});if(!r.ok)throw new Error("Artifact HTTP "+r.status);return Buffer.from(await r.arrayBuffer());}
app.listen(PORT,()=>console.log("Musab build service listening on "+PORT));