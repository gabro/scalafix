let relativePath = window.location.pathname.replace('/scalafix/docs/', '').replace('.html', '.md');
if (relativePath === '') {
  relativePath = 'index.md'
}
const repo = 'gabro/scalafix';
const branch = 'microsite';
const basePath = `https://github.com/${repo}/edit/${branch}/website/src/main/tut/docs`;
const target = `${basePath}/${relativePath}`;
const editLink = document.createElement('li');
const text = 'EDIT THIS PAGE ON GITHUB'
editLink.innerHTML = `<a href='${target}' target='_blank'><span>${text}</span></a>`
document.querySelector('.nav .pull-right').appendChild(editLink);
